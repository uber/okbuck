package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.AndroidTarget
import com.github.okbuilds.core.model.Target
import com.github.okbuilds.okbuck.rule.AndroidResourceRule

final class AndroidResourceRuleComposer {

    private AndroidResourceRuleComposer() {
        // no instance
    }

    static AndroidResourceRule compose(AndroidTarget target, AndroidTarget.ResBundle resBundle) {
        List<String> resDeps = new ArrayList<>()

        resDeps.addAll(target.compileDeps.findAll { String dep ->
            dep.endsWith(".aar")
        }.collect { String dep ->
            "//${dep.reverse().replaceFirst("/", ":").reverse()}"
        })

        target.targetCompileDeps.each { Target targetDep ->
            if (targetDep instanceof AndroidTarget) {
                targetDep.resources.each { AndroidTarget.ResBundle bundle ->
                    resDeps.add("//${targetDep.path}:res_${targetDep.name}_${bundle.id}")
                }
            }
        }

        return new AndroidResourceRule("res_${target.name}_${resBundle.id}", ["PUBLIC"], resDeps,
                target.applicationId, resBundle.resDir, resBundle.assetsDir)
    }
}
