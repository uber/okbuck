package com.uber.okbuck.core.model.base;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.LibraryExtension;
import com.android.build.gradle.api.BaseVariant;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.Var;
import com.uber.okbuck.core.model.android.AndroidAppTarget;
import com.uber.okbuck.core.model.android.AndroidLibTarget;
import com.uber.okbuck.core.model.jvm.JvmTarget;
import com.uber.okbuck.core.util.ProjectUtil;
import java.util.Map;
import javax.annotation.Nullable;
import org.gradle.api.Project;

public final class TargetCache {

  private final Project project;

  @Nullable private Map<String, Target> targets;

  public TargetCache(Project project) {
    this.project = project;
  }

  public synchronized Map<String, Target> getTargets() {
    if (targets == null) {
      ProjectType type = ProjectUtil.getType(project);
      switch (type) {
        case ANDROID_APP:
          targets =
              project
                  .getExtensions()
                  .getByType(AppExtension.class)
                  .getApplicationVariants()
                  .stream()
                  .collect(
                      ImmutableMap.toImmutableMap(
                          BaseVariant::getName, v -> new AndroidAppTarget(project, v.getName())));
          break;
        case ANDROID_LIB:
          targets =
              project
                  .getExtensions()
                  .getByType(LibraryExtension.class)
                  .getLibraryVariants()
                  .stream()
                  .collect(
                      ImmutableMap.toImmutableMap(
                          BaseVariant::getName, v -> new AndroidLibTarget(project, v.getName())));
          break;
        case KOTLIN_LIB:
          targets =
              ImmutableMap.of(
                  JvmTarget.MAIN,
                  new JvmTarget(
                      project,
                      JvmTarget.MAIN,
                      "kapt",
                      "kaptTest",
                      "kaptIntegrationTest"));
          break;
        case GROOVY_LIB:
        case SCALA_LIB:
        case JAVA_LIB:
          targets = ImmutableMap.of(JvmTarget.MAIN, new JvmTarget(project, JvmTarget.MAIN));
          break;
        default:
          targets = ImmutableMap.of();
          break;
      }
    }

    return targets;
  }

  @Nullable
  Target getTargetForVariant(@Nullable String variant) {
    @Var Target result = null;
    ProjectType type = ProjectUtil.getType(project);
    switch (type) {
      case ANDROID_LIB:
        result = getTargets().get(variant);
        if (result == null) {
          throw new IllegalStateException(
              "No target found for " + project.getDisplayName() + " for variant " + variant);
        }
        break;
      case GROOVY_LIB:
      case JAVA_LIB:
      case KOTLIN_LIB:
      case SCALA_LIB:
        result = getTargets().values().iterator().next();
        break;
      default:
        break;
    }
    return result;
  }
}
