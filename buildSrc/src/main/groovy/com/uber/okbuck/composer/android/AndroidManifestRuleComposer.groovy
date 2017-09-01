package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.core.model.android.AndroidLibTarget
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.core.model.base.Scope
import com.uber.okbuck.core.model.base.Target
import com.uber.okbuck.template.android.ManifestRule
import com.uber.okbuck.template.core.Rule

final class AndroidManifestRuleComposer extends AndroidBuckRuleComposer {

    private AndroidManifestRuleComposer() {
        // no instance
    }

    static Rule compose(AndroidAppTarget target, Scope manifestScope = target.main) {
        List<String> deps = []
        deps.addAll(external(manifestScope.externalDeps.findAll { String dep ->
            dep.endsWith("aar")
        }))

        deps.addAll(targets(manifestScope.targetDeps.findAll { Target targetDep ->
            targetDep instanceof AndroidLibTarget
        }))

        return new ManifestRule()
                .skeleton(fileRule(target.manifest))
                .name(manifest(target))
                .defaultVisibility()
                .ruleType(RuleType.ANDROID_MANIFEST.buckName)
                .deps(deps)
    }
}
