package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.AndroidTarget
import com.github.okbuilds.okbuck.rule.AndroidBuildConfigRule

final class AndroidBuildConfigRuleComposer {

    private AndroidBuildConfigRuleComposer() {
        // no instance
    }

    static AndroidBuildConfigRule compose(AndroidTarget target) {
        return new AndroidBuildConfigRule("build_config_${target.name}", ["PUBLIC"],
                target.applicationId, target.buildConfigFields)
    }
}
