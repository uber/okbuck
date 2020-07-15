package com.uber.okbuck.core.manager;

import com.google.common.collect.ImmutableList;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.OExternalDependency;
import com.uber.okbuck.core.dependency.VersionlessDependency;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.template.config.SymlinkBuckFile;
import com.uber.okbuck.template.core.Rule;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.Nullable;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;

public abstract class KotlinBaseManager {
  public static final String KOTLIN_KAPT_PLUGIN = "kotlin-kapt";
  public static final String KOTLIN_ANDROID_EXTENSIONS_MODULE = "kotlin-android-extensions";

  static final String KOTLIN_GROUP = "org.jetbrains.kotlin";
  static final String KOTLIN_HOME_BASE = "/libexec/lib";

  private static final String KOTLIN_GRADLE_MODULE = "kotlin-gradle-plugin";

  Project project;
  BuckFileManager buckFileManager;

  private boolean kotlinHomeEnabled;
  @Nullable private String kotlinVersion;
  @Nullable private Set<OExternalDependency> dependencies;

  public abstract String getTargetPath();

  public abstract String getTargetName();

  public abstract ImmutableList<VersionlessDependency> dependenciesWithTransitives();

  public abstract ImmutableList<VersionlessDependency> dependenciesWithoutTransitives();

  public abstract ImmutableList<VersionlessDependency> supportingDependencies();

  KotlinBaseManager(Project project, BuckFileManager buckFileManager) {
    this.project = project;
    this.buckFileManager = buckFileManager;
  }

  public void setup(String kotlinVersion) {
    this.kotlinHomeEnabled = true;
    this.kotlinVersion = kotlinVersion;

    Configuration kotlinConfig = project.getConfigurations().maybeCreate(getDepsConfig());
    DependencyHandler handler = project.getDependencies();
    dependenciesWithTransitives()
        .stream()
        .map(module -> String.format("%s:%s:%s", module.group(), module.name(), kotlinVersion))
        .forEach(dependency -> handler.add(getDepsConfig(), dependency));

    dependenciesWithoutTransitives()
        .stream()
        .map(
            module -> {
              DefaultExternalModuleDependency dependency =
                  new DefaultExternalModuleDependency(module.group(), module.name(), kotlinVersion);
              dependency.setTransitive(false);

              return dependency;
            })
        .forEach(dependency -> handler.add(getDepsConfig(), dependency));

    dependencies =
        new DependencyCache(project, ProjectUtil.getDependencyManager(project)).build(kotlinConfig);
  }

  public void finalizeDependencies() {
    Path path = project.file(getTargetPath()).toPath();
    FileUtil.deleteQuietly(path);

    if (!kotlinHomeEnabled) {
      // no-op if kotlin home is not enabled
      return;
    }

    if (kotlinVersion == null) {
      throw new IllegalStateException("kotlinVersion is not setup");
    }

    if (dependencies != null && dependencies.size() > 0) {
      TreeMap<String, String> targetsNameMap = new TreeMap<>();

      ImmutableList<VersionlessDependency> dependenciesToKeep =
          ImmutableList.<VersionlessDependency>builder()
              .addAll(dependenciesWithTransitives())
              .addAll(dependenciesWithoutTransitives())
              .addAll(supportingDependencies())
              .build();

      dependencies
          .stream()
          .filter(
              externalDependency ->
                  dependenciesToKeep.contains(externalDependency.getVersionless()))
          .forEach(
              externalDependency ->
                  targetsNameMap.put(
                      BuckRuleComposer.external(externalDependency),
                      externalDependency.getVersionlessTargetName()));

      Rule symlinkRule =
          new SymlinkBuckFile()
              .targetsNameMap(targetsNameMap)
              .base(KOTLIN_HOME_BASE)
              .name(getTargetName());

      buckFileManager.writeToBuckFile(
          ImmutableList.of(symlinkRule), path.resolve(OkBuckGradlePlugin.BUCK).toFile());
    }
  }

  private String getDepsConfig() {
    return "okbuck_" + getTargetName();
  }

  @Nullable
  public static String getDefaultKotlinVersion(Project project) {
    return ProjectUtil.findVersionInClasspath(project, KOTLIN_GROUP, KOTLIN_GRADLE_MODULE);
  }
}
