package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidTarget
import com.uber.okbuck.core.model.base.Target
import com.uber.okbuck.extension.OkBuckExtension
import com.uber.okbuck.rule.android.AndroidResourceRule

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
                    resDeps.add(res(targetDep as AndroidTarget, bundle))
                }
            }
        }

        OkBuckExtension okbuck = target.rootProject.okbuck
        return new AndroidResourceRule(resLocal(resBundle), ["PUBLIC"], resDeps,
                target.package, resBundle.resDir, resBundle.assetsDir, okbuck.resourceUnion)
    }
}
