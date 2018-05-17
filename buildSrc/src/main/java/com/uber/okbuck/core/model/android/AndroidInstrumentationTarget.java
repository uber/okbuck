package com.uber.okbuck.core.model.android;

import com.uber.okbuck.core.model.base.AnnotationProcessorCache;
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
    return Scope.from(getProject(), getBaseVariant().getCompileConfiguration());
  }

  @Override
  public Scope getMain() {
    return Scope.from(
        getProject(),
        getBaseVariant().getRuntimeConfiguration(),
        getSources(getBaseVariant()),
        getJavaResources(getBaseVariant()),
        AndroidTarget.getJavaCompilerOptions(getBaseVariant()));
  }

  @Override
  public Scope getTest() {
    return Scope.from(getProject());
  }

  static String getMainTargetName(String name) {
    return name.replaceFirst("_test$", "");
  }

  public static String getInstrumentationTargetName(final String name) {
    return name + "_test";
  }
}
