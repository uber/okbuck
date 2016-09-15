package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.AndroidAppTarget
import com.github.okbuilds.core.model.AndroidLibTarget
import com.github.okbuilds.core.model.Scope
import com.github.okbuilds.core.model.Target
import com.github.okbuilds.okbuck.rule.AndroidManifestRule

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
