package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.AndroidTarget
import com.github.okbuilds.okbuck.rule.AndroidBuildConfigRule

final class AndroidBuildConfigRuleComposer extends AndroidBuckRuleComposer {

    private AndroidBuildConfigRuleComposer() {
        // no instance
    }

    static AndroidBuildConfigRule compose(AndroidTarget target) {
        return new AndroidBuildConfigRule(buildConfig(target), ["PUBLIC"], target.applicationId,
                target.buildConfigFields)
    }
}
