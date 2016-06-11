package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.AndroidLibTarget
import com.github.okbuilds.core.model.AndroidTarget
import com.github.okbuilds.core.model.Target
import com.github.okbuilds.okbuck.generator.RetroLambdaGenerator
import com.github.okbuilds.okbuck.rule.AndroidLibraryRule

final class AndroidLibraryRuleComposer {

    private AndroidLibraryRuleComposer() {
        // no instance
    }

    static AndroidLibraryRule compose(AndroidLibTarget target, List<String> deps, List<String> aptDeps,
                                      List<String> aidlRuleNames, String appClass) {
        deps.addAll(target.compileDeps.collect { String dep ->
            "//${dep.reverse().replaceFirst("/", ":").reverse()}"
        })

        deps.addAll(target.targetCompileDeps.collect { Target targetDep ->
            "//${targetDep.path}:src_${targetDep.name}"
        })

        target.targetCompileDeps.each { Target targetDep ->
            if (targetDep instanceof AndroidTarget) {
                targetDep.resources.each { AndroidTarget.ResBundle bundle ->
                    deps.add("//${targetDep.path}:res_${targetDep.name}_${bundle.id}")
                }
            }
        }

        List<String> postprocessClassesCommands = []
        if (target.retrolambda) {
            postprocessClassesCommands.add(RetroLambdaGenerator.generate(target))
        }

        return new AndroidLibraryRule("src_${target.name}", ["PUBLIC"], deps, target.sources,
                target.manifest, target.annotationProcessors as List, aptDeps, aidlRuleNames,
                appClass, target.sourceCompatibility, target.targetCompatibility,
                postprocessClassesCommands, target.jvmArgs)
    }
}
