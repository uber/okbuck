package com.uber.okbuck.core.manager;

import com.google.common.collect.ImmutableList;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.VersionlessDependency;
import org.gradle.api.Project;

public final class KotlinPluginManager extends KotlinBaseManager {

  public static final String KOTLIN_ALLOPEN_MODULE = "kotlin-allopen";
  public static final String KOTLIN_ALLOPEN_JAR = KOTLIN_ALLOPEN_MODULE + ".jar";

  static final String KOTLIN_PLUGIN_HOME = "kotlin_plugin_home";
  static final String KOTLIN_PLUGIN_HOME_LOCATION =
      OkBuckGradlePlugin.WORKSPACE_PATH + "/" + KOTLIN_PLUGIN_HOME;

  public static final String KOTLIN_LIBRARIES_LOCATION =
      "buck-out/gen"
          + "/"
          + KOTLIN_PLUGIN_HOME_LOCATION
          + "/"
          + KOTLIN_PLUGIN_HOME
          + KOTLIN_HOME_BASE;

  public KotlinPluginManager(Project project, BuckFileManager buckFileManager) {
    super(project, buckFileManager);
  }

  @Override
  public String getTargetPath() {
    return KOTLIN_PLUGIN_HOME_LOCATION;
  }

  @Override
  public String getTargetName() {
    return KOTLIN_PLUGIN_HOME;
  }

  @Override
  public ImmutableList<VersionlessDependency> dependenciesWithTransitives() {
    return ImmutableList.of();
  }

  @Override
  public ImmutableList<VersionlessDependency> dependenciesWithoutTransitives() {
    return ImmutableList.of(
        VersionlessDependency.builder()
            .setGroup(KOTLIN_GROUP)
            .setName(KOTLIN_ALLOPEN_MODULE)
            .build(),
        VersionlessDependency.builder()
            .setGroup(KOTLIN_GROUP)
            .setName("kotlin-scripting-compiler-impl")
            .build(),
        VersionlessDependency.builder()
            .setGroup(KOTLIN_GROUP)
            .setName("kotlin-scripting-compiler")
            .build(),
        VersionlessDependency.builder()
            .setGroup(KOTLIN_GROUP)
            .setName("kotlin-scripting-jvm")
            .build(),
        VersionlessDependency.builder()
            .setGroup(KOTLIN_GROUP)
            .setName("kotlin-scripting-common")
            .build());
  }

  @Override
  public ImmutableList<VersionlessDependency> supportingDependencies() {
    return ImmutableList.of();
  }
}
