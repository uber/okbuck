package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.AndroidAppTarget
import com.github.okbuilds.okbuck.rule.AndroidInstrumentationTestRule

final class AndroidInstrumentationTestRuleComposer extends AndroidBuckRuleComposer {

    private AndroidInstrumentationTestRuleComposer() {
        // no instance
    }

    static AndroidInstrumentationTestRule compose(AndroidAppTarget mainApkTarget) {
        return new AndroidInstrumentationTestRule(instrumentationTest(mainApkTarget), instrumentation(mainApkTarget))
    }
}
