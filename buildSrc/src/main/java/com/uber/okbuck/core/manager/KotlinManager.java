package com.uber.okbuck.core.manager;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;

public final class KotlinManager {

  public static final String KOTLIN_HOME_LOCATION =
      OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/kotlin_home";
  public static final String KOTLIN_ANDROID_EXTENSIONS_MODULE = "kotlin-android-extensions";
  public static final String KOTLIN_ALLOPEN_MODULE = "kotlin-allopen";
  public static final String KOTLIN_KAPT_PLUGIN = "kotlin-kapt";
  public static final String KOTLIN_LIBRARIES_LOCATION = KOTLIN_HOME_LOCATION + "/libexec/lib";
  public static final String KOTLIN_LIBRARIES_CACHE_LOCATION = "3rdparty/org/jetbrains/kotlin";

  private static final String KOTLIN_DEPS_CONFIG = "okbuck_kotlin_deps";
  private static final String KOTLIN_GROUP = "org.jetbrains.kotlin";

  private static final String KOTLIN_COMPILER_MODULE = "kotlin-compiler-embeddable";
  private static final String KOTLIN_GRADLE_MODULE = "kotlin-gradle-plugin";
  private static final String KOTLIN_GRADLE_MODULE_API = "kotlin-gradle-plugin-api";
  private static final String KOTLIN_STDLIB_MODULE = "kotlin-stdlib";
  private static final String KOTLIN_STDLIB_COMMON_MODULE = "kotlin-stdlib-common";
  private static final String KOTLIN_REFLECT_MODULE = "kotlin-reflect";
  private static final String KOTLIN_SCRIPT_RUNTIME_MODULE = "kotlin-script-runtime";
  private static final String KOTLIN_ANNOTATION_PROCESSING_MODULE =
      "kotlin-annotation-processing-gradle";

  private final Project project;

  private String kotlinVersion;

  public KotlinManager(Project project) {
    this.project = project;
  }

  public static String getDefaultKotlinVersion(Project project) {
    return ProjectUtil.findVersionInClasspath(project, KOTLIN_GROUP, KOTLIN_GRADLE_MODULE);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void setupKotlinHome(String kotlinVersion) {

    this.kotlinVersion = kotlinVersion;

    Configuration kotlinConfig = project.getConfigurations().maybeCreate(KOTLIN_DEPS_CONFIG);
    DependencyHandler handler = project.getDependencies();
    handler.add(
        KOTLIN_DEPS_CONFIG,
        String.format("%s:%s:%s", KOTLIN_GROUP, KOTLIN_COMPILER_MODULE, kotlinVersion));
    handler.add(
        KOTLIN_DEPS_CONFIG,
        String.format("%s:%s:%s", KOTLIN_GROUP, KOTLIN_STDLIB_MODULE, kotlinVersion));
    handler.add(
        KOTLIN_DEPS_CONFIG,
        String.format("%s:%s:%s", KOTLIN_GROUP, KOTLIN_ANDROID_EXTENSIONS_MODULE, kotlinVersion));
    handler.add(
        KOTLIN_DEPS_CONFIG,
        String.format("%s:%s:%s", KOTLIN_GROUP, KOTLIN_ALLOPEN_MODULE, kotlinVersion));
    handler.add(
        KOTLIN_DEPS_CONFIG,
        String.format("%s:%s:%s", KOTLIN_GROUP, KOTLIN_REFLECT_MODULE, kotlinVersion));
    handler.add(
        KOTLIN_DEPS_CONFIG,
        String.format("%s:%s:%s", KOTLIN_GROUP, KOTLIN_SCRIPT_RUNTIME_MODULE, kotlinVersion));
    handler.add(
        KOTLIN_DEPS_CONFIG,
        String.format(
            "%s:%s:%s", KOTLIN_GROUP, KOTLIN_ANNOTATION_PROCESSING_MODULE, kotlinVersion));

    new DependencyCache(project, ProjectUtil.getDependencyManager(project)).build(kotlinConfig);
  }

  public void finalizeDependencies() {
    Path fromPath = project.file(KOTLIN_LIBRARIES_CACHE_LOCATION).toPath();
    Path toPath = project.file(KOTLIN_LIBRARIES_LOCATION).toPath();

    FileUtil.deleteQuietly(toPath);
    toPath.toFile().mkdirs();

    copyFile(fromPath, toPath, KOTLIN_ALLOPEN_MODULE, kotlinVersion);
    copyFile(fromPath, toPath, KOTLIN_ANDROID_EXTENSIONS_MODULE, kotlinVersion);
    copyFile(fromPath, toPath, KOTLIN_ANNOTATION_PROCESSING_MODULE, kotlinVersion);
    copyFile(fromPath, toPath, KOTLIN_COMPILER_MODULE, kotlinVersion);
    copyFile(fromPath, toPath, KOTLIN_GRADLE_MODULE_API, kotlinVersion);
    copyFile(fromPath, toPath, KOTLIN_REFLECT_MODULE, kotlinVersion);
    copyFile(fromPath, toPath, KOTLIN_SCRIPT_RUNTIME_MODULE, kotlinVersion);
    copyFile(fromPath, toPath, KOTLIN_STDLIB_MODULE, kotlinVersion);
    copyFile(fromPath, toPath, KOTLIN_STDLIB_COMMON_MODULE, kotlinVersion);
  }

  private static void copyFile(Path fromPath, Path toPath, String name, String version) {

    Path fromFilePath = fromPath.resolve(name + "--" + version + ".jar");

    if (!fromFilePath.toFile().exists()) {
      fromFilePath = fromPath.resolve(name + ".jar");
    }

    Path toFilePath = toPath.resolve(name + ".jar");

    try {
      Files.createLink(toFilePath, fromFilePath);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
