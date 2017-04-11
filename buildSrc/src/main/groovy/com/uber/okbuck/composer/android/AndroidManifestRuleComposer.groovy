package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.core.model.android.AndroidLibTarget
import com.uber.okbuck.core.model.base.Scope
import com.uber.okbuck.core.model.base.Target
import com.uber.okbuck.rule.android.AndroidManifestRule

final class AndroidManifestRuleComposer extends AndroidBuckRuleComposer {

    private AndroidManifestRuleComposer() {
        // no instance
    }

    static AndroidManifestRule compose(AndroidAppTarget target, Scope manifestScope = target.main) {
        List<String> deps = []
        deps.addAll(external(manifestScope.externalDeps.findAll { String dep ->
            dep.endsWith("aar")
        }, target))

        deps.addAll(targets(manifestScope.targetDeps.findAll { Target targetDep ->
            targetDep instanceof AndroidLibTarget
        }))

        return new AndroidManifestRule(manifest(target), ["PUBLIC"], deps, fileRule(target.manifest, target))
    }
}
