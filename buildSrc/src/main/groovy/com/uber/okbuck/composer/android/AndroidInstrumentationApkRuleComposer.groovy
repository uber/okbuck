package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.rule.android.AndroidInstrumentationApkRule

final class AndroidInstrumentationApkRuleComposer extends AndroidBuckRuleComposer {

    private AndroidInstrumentationApkRuleComposer() {
        // no instance
    }

    static AndroidInstrumentationApkRule compose(List<String> deps,
                                                 String manifestRuleName,
                                                 AndroidAppTarget mainApkTarget) {
        return new AndroidInstrumentationApkRule(
                instrumentation(mainApkTarget),
                ["PUBLIC"],
                deps,
                manifestRuleName,
                bin(mainApkTarget))
    }
}
