package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.AndroidAppTarget
import com.github.okbuilds.core.model.AndroidLibTarget
import com.github.okbuilds.core.model.Target
import com.github.okbuilds.okbuck.rule.AndroidManifestRule

final class AndroidManifestRuleComposer {

    private AndroidManifestRuleComposer() {
        // no instance
    }

    static AndroidManifestRule compose(AndroidAppTarget target) {
        List<String> deps = []

        deps.addAll(target.compileDeps.findAll { String dep ->
            dep.endsWith("aar")
        }.collect { String dep ->
            "//${dep.reverse().replaceFirst("/", ":").reverse()}"
        })

        deps.addAll(target.targetCompileDeps.findAll { Target targetDep ->
            targetDep instanceof AndroidLibTarget
        }.collect { Target targetDep ->
            "//${targetDep.path}:src_${targetDep.name}"
        })

        return new AndroidManifestRule("manifest_${target.name}", ["PUBLIC"], deps, target.manifest)
    }
}
