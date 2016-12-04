package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.rule.android.AndroidInstrumentationTestRule

final class AndroidInstrumentationTestRuleComposer extends AndroidBuckRuleComposer {

    private AndroidInstrumentationTestRuleComposer() {
        // no instance
    }

    static AndroidInstrumentationTestRule compose(AndroidAppTarget mainApkTarget) {
        return new AndroidInstrumentationTestRule(instrumentationTest(mainApkTarget), instrumentation(mainApkTarget))
    }
}
