package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.core.model.android.AndroidAppInstrumentationTarget
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.template.android.InstrumentationApkRule
import com.uber.okbuck.template.core.Rule

final class AndroidInstrumentationApkRuleComposer extends AndroidBuckRuleComposer {

    private AndroidInstrumentationApkRuleComposer() {
        // no instance
    }

    static Rule compose(List<String> deps,
                        AndroidAppInstrumentationTarget target,
                        AndroidAppTarget mainApkTarget) {
        return new InstrumentationApkRule()
                .manifest(fileRule(target.manifest))
                .mainApkRuleName(bin(mainApkTarget))
                .defaultVisibility()
                .ruleType(RuleType.ANDROID_INSTRUMENTATION_APK.buckName)
                .name(instrumentation(mainApkTarget))
                .deps(deps)
    }
}
