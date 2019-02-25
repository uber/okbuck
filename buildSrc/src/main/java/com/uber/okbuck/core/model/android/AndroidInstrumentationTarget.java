package com.uber.okbuck.core.model.android;

import com.uber.okbuck.core.annotation.AnnotationProcessorCache;
import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.core.util.ProjectUtil;
import java.util.List;
import org.gradle.api.Project;

/** An abstract Android instrumentation target */
public abstract class AndroidInstrumentationTarget extends AndroidAppTarget {

  private static final String KAPT_ANDROID_TEST = "kaptAndroidTest";

  public AndroidInstrumentationTarget(Project project, String name) {
    super(project, name, true);
  }

  // TODO: Update to use variant once issue solved: https://youtrack.jetbrains.com/issue/KT-23411
  @Override
  public List<Scope> getAptScopes() {
    AnnotationProcessorCache apCache = ProjectUtil.getAnnotationProcessorCache(getProject());
    if (getIsKapt()) {
      return apCache.getAnnotationProcessorScopes(getProject(), KAPT_ANDROID_TEST);
    } else {
      return apCache.getAnnotationProcessorScopes(
          getProject(), getBaseVariant().getAnnotationProcessorConfiguration());
    }
  }

  @Override
  public Scope getApt() {
    if (getIsKapt()) {
      return getAptScopeForConfiguration(KAPT_ANDROID_TEST);
    } else {
      return getAptScopeForConfiguration(getBaseVariant().getAnnotationProcessorConfiguration());
    }
  }

  @Override
  public Scope getProvided() {
    return Scope.builder(getProject())
        .configuration(getBaseVariant().getCompileConfiguration())
        .build();
  }

  @Override
  public Scope getMain() {
    return Scope.builder(getProject())
        .configuration(getBaseVariant().getRuntimeConfiguration())
        .sourceDirs(getSources(getBaseVariant()))
        .javaResourceDirs(getJavaResources(getBaseVariant()))
        .customOptions(JAVA_COMPILER_EXTRA_ARGUMENTS, getJavaCompilerOptions(getBaseVariant()))
        .customOptions(KOTLIN_COMPILER_EXTRA_ARGUMENTS, getKotlinCompilerOptions())
        .customOptions(getKotlinFriendPaths(false))
        .build();
  }

  @Override
  public Scope getTest() {
    return Scope.builder(getProject()).build();
  }

  static String getMainTargetName(String name) {
    return name.replaceFirst("_test$", "");
  }

  static String getInstrumentationTargetName(String name) {
    return name + "_test";
  }
}
