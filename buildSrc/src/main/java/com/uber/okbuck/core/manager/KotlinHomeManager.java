package com.uber.okbuck.core.manager;

import com.google.common.collect.ImmutableList;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.VersionlessDependency;
import org.gradle.api.Project;

public final class KotlinHomeManager extends KotlinBaseManager {

  private static final String KOTLIN_HOME = "kotlin_home";
  private static final String KOTLIN_HOME_LOCATION =
      OkBuckGradlePlugin.WORKSPACE_PATH + "/" + KOTLIN_HOME;
  public static final String KOTLIN_HOME_TARGET = "//" + KOTLIN_HOME_LOCATION + ":" + KOTLIN_HOME;

  public KotlinHomeManager(Project project, BuckFileManager buckFileManager) {
    super(project, buckFileManager);
  }

  @Override
  public String getTargetPath() {
    return KOTLIN_HOME_LOCATION;
  }

  @Override
  public String getTargetName() {
    return KOTLIN_HOME;
  }

  @Override
  public ImmutableList<VersionlessDependency> dependenciesWithTransitives() {
    return ImmutableList.of(
        VersionlessDependency.builder().setGroup(KOTLIN_GROUP).setName("kotlin-compiler").build(),
        VersionlessDependency.builder().setGroup(KOTLIN_GROUP).setName("kotlin-reflect").build(),
        VersionlessDependency.builder().setGroup(KOTLIN_GROUP).setName("kotlin-stdlib").build(),
        VersionlessDependency.builder()
            .setGroup(KOTLIN_GROUP)
            .setName("kotlin-script-runtime")
            .build(),
        VersionlessDependency.builder().setGroup(KOTLIN_GROUP).setName("jvm-abi-gen").build());
  }

  @Override
  public ImmutableList<VersionlessDependency> dependenciesWithoutTransitives() {
    return ImmutableList.of(
        VersionlessDependency.builder()
            .setGroup(KOTLIN_GROUP)
            .setName("kotlin-annotation-processing")
            .build());
  }

  @Override
  public ImmutableList<VersionlessDependency> supportingDependencies() {
    return ImmutableList.of(
        VersionlessDependency.builder()
            .setGroup("org.jetbrains.intellij.deps")
            .setName("trove4j")
            .build(),
        VersionlessDependency.builder().setGroup("org.jetbrains").setName("annotations").build());
  }
}
