package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidTarget
import com.uber.okbuck.rule.android.AndroidBuildConfigRule

final class AndroidBuildConfigRuleComposer extends AndroidBuckRuleComposer {

    private AndroidBuildConfigRuleComposer() {
        // no instance
    }

    static AndroidBuildConfigRule compose(AndroidTarget target) {
        return new AndroidBuildConfigRule(buildConfig(target), ["PUBLIC"], target.package,
                target.buildConfigFields)
    }
}
