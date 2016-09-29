package com.uber.okbuck.composer

import com.uber.okbuck.core.model.AndroidAppTarget
import com.uber.okbuck.rule.AndroidInstrumentationTestRule

final class AndroidInstrumentationTestRuleComposer extends AndroidBuckRuleComposer {

    private AndroidInstrumentationTestRuleComposer() {
        // no instance
    }

    static AndroidInstrumentationTestRule compose(AndroidAppTarget mainApkTarget) {
        return new AndroidInstrumentationTestRule(instrumentationTest(mainApkTarget), instrumentation(mainApkTarget))
    }
}
