package com.uber.okbuck.core.model.base;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.LibraryExtension;
import com.android.build.gradle.api.BaseVariant;
import com.uber.okbuck.core.model.android.AndroidAppTarget;
import com.uber.okbuck.core.model.android.AndroidLibTarget;
import com.uber.okbuck.core.model.jvm.JvmTarget;
import com.uber.okbuck.core.util.ProjectUtil;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.gradle.api.Project;

public class TargetCache {

  private final Map<Project, Map<String, Target>> store = new HashMap<>();
  private final Map<Project, Map<String, Target>> artifactNameToTarget = new HashMap<>();
  private final Map<String, String> lintConfig = new ConcurrentHashMap<>();

  public Map<String, Target> getTargets(Project project) {
    Map<String, Target> projectTargets = store.get(project);
    if (projectTargets == null) {
      ProjectType type = ProjectUtil.getType(project);
      switch (type) {
        case ANDROID_APP:
          projectTargets = new HashMap<>();
          for (BaseVariant v :
              project.getExtensions().getByType(AppExtension.class).getApplicationVariants()) {
            projectTargets.put(v.getName(), new AndroidAppTarget(project, v.getName()));
          }
          break;
        case ANDROID_LIB:
          projectTargets = new HashMap<>();
          Map<String, Target> projectArtifacts = new HashMap<>();
          for (BaseVariant v :
              project.getExtensions().getByType(LibraryExtension.class).getLibraryVariants()) {
            Target target = new AndroidLibTarget(project, v.getName());
            projectTargets.put(v.getName(), target);

            projectArtifacts.put(v.getName(), target);
          }
          artifactNameToTarget.put(project, projectArtifacts);
          break;
        case KOTLIN_LIB:
          projectTargets =
              Collections.singletonMap(
                  JvmTarget.MAIN, new JvmTarget(project, JvmTarget.MAIN, "kapt", "kaptTest"));
          break;
        case GROOVY_LIB:
        case SCALA_LIB:
        case JAVA_LIB:
          projectTargets =
              Collections.singletonMap(JvmTarget.MAIN, new JvmTarget(project, JvmTarget.MAIN));
          break;
        default:
          projectTargets = Collections.emptyMap();
          break;
      }
      store.put(project, projectTargets);
    }

    return projectTargets;
  }

  @Nullable
  public Target getTargetForVariant(Project targetProject, @Nullable String variant) {
    Target result;
    ProjectType type = ProjectUtil.getType(targetProject);
    switch (type) {
      case ANDROID_LIB:
        result = artifactNameToTarget.get(targetProject).get(variant);
        if (result == null) {
          throw new IllegalStateException(
              "No target found for " + targetProject.getDisplayName() + " for variant " + variant);
        }
        break;
      case GROOVY_LIB:
      case JAVA_LIB:
      case KOTLIN_LIB:
      case SCALA_LIB:
        result = getTargets(targetProject).values().iterator().next();
        break;
      default:
        result = null;
    }
    return result;
  }
}
