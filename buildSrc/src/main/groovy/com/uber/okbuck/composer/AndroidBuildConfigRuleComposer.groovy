package com.uber.okbuck.composer

import com.uber.okbuck.core.model.AndroidTarget
import com.uber.okbuck.rule.AndroidBuildConfigRule

final class AndroidBuildConfigRuleComposer extends AndroidBuckRuleComposer {

    private AndroidBuildConfigRuleComposer() {
        // no instance
    }

    static AndroidBuildConfigRule compose(AndroidTarget target) {
        return new AndroidBuildConfigRule(buildConfig(target), ["PUBLIC"], target.applicationId,
                target.buildConfigFields)
    }
}
