package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.AndroidAppTarget
import com.github.okbuilds.okbuck.rule.AndroidInstrumentationApkRule

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
