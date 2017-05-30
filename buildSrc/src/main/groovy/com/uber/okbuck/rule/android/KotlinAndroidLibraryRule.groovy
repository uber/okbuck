package com.uber.okbuck.rule.android;

import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.core.model.jvm.TestOptions;
import java.util.List;
import java.util.Set;

final class KotlinAndroidLibraryRule extends AndroidRule {
  /**
   * @srcTargets , used for SqlDelight support(or other case), genrule's output will be used as src, pass empty set if not present
   * @param appClass , if exopackage is enabled, pass the detected app class, otherwise, pass null
   * */
  KotlinAndroidLibraryRule(
      String name,
      List<String> visibility,
      List<String> deps,
      Set<String> srcSet,
      String manifest,
      List<String> annotationProcessors,
      List<String> aptDeps,
      Set<String> providedDeps,
      List<String> aidlRuleNames,
      String appClass,
      String sourceCompatibility,
      String targetCompatibility,
      List<String> postprocessClassesCommands,
      List<String> options,
      boolean generateR2,
      List<String> testTargets,
      Set<String> extraOpts) {

    super(
        RuleType.KOTLIN_ANDROID_LIBRARY,
        name,
        visibility,
        deps,
        srcSet,
        manifest,
        null,
        annotationProcessors,
        aptDeps,
        providedDeps,
        aidlRuleNames,
        appClass,
        sourceCompatibility,
        targetCompatibility,
        postprocessClassesCommands,
        options,
        TestOptions.EMPTY,
        generateR2,
        null,
        null,
        testTargets,
        null,
        extraOpts);
  }
}
