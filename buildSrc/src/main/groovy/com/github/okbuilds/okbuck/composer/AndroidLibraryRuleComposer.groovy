package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.AndroidLibTarget
import com.github.okbuilds.core.model.AndroidTarget
import com.github.okbuilds.core.model.Target
import com.github.okbuilds.okbuck.generator.RetroLambdaGenerator
import com.github.okbuilds.okbuck.rule.AndroidLibraryRule

final class AndroidLibraryRuleComposer extends AndroidBuckRuleComposer {

    private AndroidLibraryRuleComposer() {
        // no instance
    }

    static AndroidLibraryRule compose(AndroidLibTarget target, List<String> deps,
                                      List<String> aptDeps, List<String> aidlRuleNames,
                                      String appClass) {
        deps.addAll(external(target.compileDeps))
        deps.addAll(targets(target.targetCompileDeps))

        target.targetCompileDeps.each { Target targetDep ->
            if (targetDep instanceof AndroidTarget) {
                targetDep.resources.each { AndroidTarget.ResBundle bundle ->
                    deps.add(res(targetDep, bundle))
                }
            }
        }

        List<String> postprocessClassesCommands = []
        if (target.retrolambda) {
            postprocessClassesCommands.add(RetroLambdaGenerator.generate(target))
        }

        return new AndroidLibraryRule(src(target), ["PUBLIC"], deps, target.sources,
                target.manifest, target.annotationProcessors as List, aptDeps, aidlRuleNames,
                appClass, target.sourceCompatibility, target.targetCompatibility,
                postprocessClassesCommands, target.jvmArgs)
    }
}
