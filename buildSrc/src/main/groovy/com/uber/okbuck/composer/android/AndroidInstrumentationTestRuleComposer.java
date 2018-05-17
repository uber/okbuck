package com.uber.okbuck.composer.android;

import com.uber.okbuck.core.model.android.AndroidAppTarget;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.template.android.InstrumentationTestRule;
import com.uber.okbuck.template.core.Rule;

public final class AndroidInstrumentationTestRuleComposer extends AndroidBuckRuleComposer {

  private AndroidInstrumentationTestRuleComposer() {
    // no instance
  }

  public static Rule compose(AndroidAppTarget mainApkTarget) {
    return new InstrumentationTestRule()
        .instrumentationApkRuleName(instrumentation(mainApkTarget))
        .ruleType(RuleType.ANDROID_INSTRUMENTATION_TEST.getBuckName())
        .name(instrumentationTest(mainApkTarget));
  }
}
