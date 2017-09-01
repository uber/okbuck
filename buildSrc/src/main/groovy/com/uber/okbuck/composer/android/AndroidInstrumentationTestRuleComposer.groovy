package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.template.android.InstrumentationTestRule
import com.uber.okbuck.template.core.Rule

final class AndroidInstrumentationTestRuleComposer extends AndroidBuckRuleComposer {

    private AndroidInstrumentationTestRuleComposer() {
        // no instance
    }

    static Rule compose(AndroidAppTarget mainApkTarget) {
        return new InstrumentationTestRule()
                .instrumentationApkRuleName(instrumentation(mainApkTarget))
                .ruleType(RuleType.ANDROID_INSTRUMENTATION_TEST.buckName)
                .name(instrumentationTest(mainApkTarget))
    }
}
