package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.AndroidTarget
import com.github.okbuilds.core.model.Target
import com.github.okbuilds.okbuck.rule.AndroidResourceRule

final class AndroidResourceRuleComposer extends AndroidBuckRuleComposer {

    private AndroidResourceRuleComposer() {
        // no instance
    }

    static AndroidResourceRule compose(AndroidTarget target, AndroidTarget.ResBundle resBundle) {
        List<String> resDeps = new ArrayList<>()

        resDeps.addAll(external(target.main.externalDeps.findAll { String dep ->
            dep.endsWith(".aar")
        }))

        target.main.targetDeps.each { Target targetDep ->
            if (targetDep instanceof AndroidTarget) {
                targetDep.resources.each { AndroidTarget.ResBundle bundle ->
                    resDeps.add(res(targetDep, bundle))
                }
            }
        }

        return new AndroidResourceRule(resLocal(target, resBundle), ["PUBLIC"], resDeps,
                target.applicationId, resBundle.resDir, resBundle.assetsDir)
    }
}
