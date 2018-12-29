package com.uber.okbuck.core.manager;

import com.google.common.collect.ImmutableList;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.template.config.SymlinkBuckFile;
import com.uber.okbuck.template.core.Rule;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;

public final class KotlinManager {
  public static final String KOTLIN_ANDROID_EXTENSIONS_MODULE = "kotlin-android-extensions";
  public static final String KOTLIN_ALLOPEN_MODULE = "kotlin-allopen";
  public static final String KOTLIN_ALLOPEN_JAR = KOTLIN_ALLOPEN_MODULE + ".jar";

  public static final String KOTLIN_HOME = "kotlin_home";
  public static final String KOTLIN_HOME_LOCATION =
      OkBuckGradlePlugin.WORKSPACE_PATH + "/" + KOTLIN_HOME;
  public static final String KOTLIN_HOME_TARGET = "//" + KOTLIN_HOME_LOCATION + ":" + KOTLIN_HOME;
  public static final String KOTLIN_KAPT_PLUGIN = "kotlin-kapt";
  public static final String KOTLIN_HOME_BASE = "/libexec/lib";
  public static final String KOTLIN_LIBRARIES_LOCATION =
      "buck-out/gen" + "/" + KOTLIN_HOME_LOCATION + "/" + KOTLIN_HOME + KOTLIN_HOME_BASE;
  private static final String KOTLIN_GROUP = "org.jetbrains.kotlin";
  private static final String KOTLIN_GRADLE_MODULE = "kotlin-gradle-plugin";
  private static final String KOTLIN_DEPS_CONFIG = "okbuck_kotlin_deps";

  private static final ImmutableList<String> kotlinModules =
      ImmutableList.of(
          "kotlin-compiler-embeddable",
          "kotlin-stdlib",
          KOTLIN_ANDROID_EXTENSIONS_MODULE,
          KOTLIN_ALLOPEN_MODULE,
          "kotlin-reflect",
          "kotlin-script-runtime",
          "kotlin-annotation-processing-gradle",
          "kotlin-gradle-plugin-api",
          "kotlin-stdlib-common");

  private final Project project;
  private final BuckFileManager buckFileManager;

  @Nullable private String kotlinVersion;
  @Nullable private Set<ExternalDependency> dependencies;

  public KotlinManager(Project project, BuckFileManager buckFileManager) {
    this.project = project;
    this.buckFileManager = buckFileManager;
  }

  @Nullable
  public static String getDefaultKotlinVersion(Project project) {
    return ProjectUtil.findVersionInClasspath(project, KOTLIN_GROUP, KOTLIN_GRADLE_MODULE);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void setupKotlinHome(String kotlinVersion) {
    this.kotlinVersion = kotlinVersion;

    Configuration kotlinConfig = project.getConfigurations().maybeCreate(KOTLIN_DEPS_CONFIG);
    DependencyHandler handler = project.getDependencies();
    kotlinModules
        .stream()
        .map(module -> String.format("%s:%s:%s", KOTLIN_GROUP, module, kotlinVersion))
        .forEach(dependency -> handler.add(KOTLIN_DEPS_CONFIG, dependency));

    dependencies =
        new DependencyCache(project, ProjectUtil.getDependencyManager(project)).build(kotlinConfig);
  }

  public void finalizeDependencies() {
    if (kotlinVersion == null) {
      throw new IllegalStateException("kotlinVersion is not setup");
    }

    Path path = project.file(KOTLIN_HOME_LOCATION).toPath();
    FileUtil.deleteQuietly(path);

    if (dependencies != null && dependencies.size() > 0) {
      Map<String, String> targetsNameMap =
          dependencies
              .stream()
              .collect(
                  Collectors.toMap(
                      BuckRuleComposer::external, ExternalDependency::getVersionlessTargetName));
      Rule symlinkRule =
          new SymlinkBuckFile()
              .targetsNameMap(targetsNameMap)
              .base(KOTLIN_HOME_BASE)
              .name(KOTLIN_HOME);
      buckFileManager.writeToBuckFile(
          ImmutableList.of(symlinkRule), path.resolve(OkBuckGradlePlugin.BUCK).toFile());
    }
  }
}
