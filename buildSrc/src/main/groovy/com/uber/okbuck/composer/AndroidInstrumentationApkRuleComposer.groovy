package com.uber.okbuck.composer

import com.uber.okbuck.core.model.AndroidAppTarget
import com.uber.okbuck.rule.AndroidInstrumentationApkRule

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
