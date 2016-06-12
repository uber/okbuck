package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.AndroidAppTarget
import com.github.okbuilds.core.model.AndroidLibTarget
import com.github.okbuilds.core.model.Target
import com.github.okbuilds.okbuck.rule.AndroidManifestRule

final class AndroidManifestRuleComposer extends AndroidBuckRuleComposer {

    private AndroidManifestRuleComposer() {
        // no instance
    }

    static AndroidManifestRule compose(AndroidAppTarget target) {
        List<String> deps = []

        deps.addAll(external(target.compileDeps.findAll { String dep ->
            dep.endsWith("aar")
        }))

        deps.addAll(targets(target.targetCompileDeps.findAll { Target targetDep ->
            targetDep instanceof AndroidLibTarget
        }))

        return new AndroidManifestRule(manifest(target), ["PUBLIC"], deps, target.manifest)
    }
}
