package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidTarget
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.template.android.BuildConfigRule
import com.uber.okbuck.template.core.Rule

final class AndroidBuildConfigRuleComposer extends AndroidBuckRuleComposer {

    private AndroidBuildConfigRuleComposer() {
        // no instance
    }

    static Rule compose(AndroidTarget target) {
        return new BuildConfigRule()
                .pkg(target.package)
                .values(target.buildConfigFields)
                .defaultVisibility()
                .ruleType(RuleType.ANDROID_BUILD_CONFIG.buckName)
                .name(buildConfig(target))
    }
}
