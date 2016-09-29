package com.uber.okbuck.composer

import com.uber.okbuck.core.model.AndroidAppTarget
import com.uber.okbuck.core.model.AndroidLibTarget
import com.uber.okbuck.core.model.Scope
import com.uber.okbuck.core.model.Target
import com.uber.okbuck.rule.AndroidManifestRule

final class AndroidManifestRuleComposer extends AndroidBuckRuleComposer {

    private AndroidManifestRuleComposer() {
        // no instance
    }

    static AndroidManifestRule compose(AndroidAppTarget target, Scope manifestScope = target.main) {
        List<String> deps = []

        deps.addAll(external(manifestScope.externalDeps.findAll { String dep ->
            dep.endsWith("aar")
        }))

        deps.addAll(targets(manifestScope.targetDeps.findAll { Target targetDep ->
            targetDep instanceof AndroidLibTarget
        }))

        return new AndroidManifestRule(manifest(target), ["PUBLIC"], deps, target.manifest)
    }
}
