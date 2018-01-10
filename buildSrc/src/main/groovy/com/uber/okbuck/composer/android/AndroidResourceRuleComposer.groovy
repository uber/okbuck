package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidTarget
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.core.model.base.Target
import com.uber.okbuck.extension.OkBuckExtension
import com.uber.okbuck.template.android.ResourceRule
import com.uber.okbuck.template.core.Rule

final class AndroidResourceRuleComposer extends AndroidBuckRuleComposer {

    private AndroidResourceRuleComposer() {
        // no instance
    }

    static Rule compose(AndroidTarget target) {
        List<String> resDeps = []
        resDeps.addAll(external(target.main.externalDeps.findAll { String dep ->
            dep.endsWith(".aar")
        }))

        resDeps.addAll(getTargetDeps(target.main, target.provided).findAll { Target targetDep ->
            targetDep instanceof AndroidTarget
        }.collect { Target targetDep ->
            resRule(targetDep as AndroidTarget)
        })

        OkBuckExtension okbuck = target.rootProject.okbuck

        return new ResourceRule()
                .pkg(target.package)
                .res(target.resDirs)
                .assets(target.assetDirs)
                .resourceUnion(okbuck.resourceUnion)
                .defaultVisibility()
                .ruleType(RuleType.ANDROID_RESOURCE.buckName)
                .deps(resDeps)
                .name(res(target))
    }
}
