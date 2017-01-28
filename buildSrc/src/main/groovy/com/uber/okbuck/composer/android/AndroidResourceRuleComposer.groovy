package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidTarget
import com.uber.okbuck.core.model.base.Target
import com.uber.okbuck.extension.OkBuckExtension
import com.uber.okbuck.rule.android.AndroidResourceRule

final class AndroidResourceRuleComposer extends AndroidBuckRuleComposer {

    private AndroidResourceRuleComposer() {
        // no instance
    }

    static AndroidResourceRule compose(AndroidTarget target) {
        List<String> resDeps = []
        resDeps.addAll(external(target.main.externalDeps.findAll { String dep ->
            dep.endsWith(".aar")
        }))

        resDeps.addAll(target.main.targetDeps.findAll { Target targetDep ->
            targetDep instanceof AndroidTarget
        }.collect { Target targetDep ->
            resRule(targetDep as AndroidTarget)
        })

        OkBuckExtension okbuck = target.rootProject.okbuck
        return new AndroidResourceRule(
                res(target),
                ["PUBLIC"],
                resDeps,
                target.package,
                target.resDirs,
                target.assetDirs,
                okbuck.resourceUnion)
    }
}
