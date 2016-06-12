package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.JavaLibTarget
import com.github.okbuilds.core.model.Target
import com.github.okbuilds.okbuck.generator.RetroLambdaGenerator
import com.github.okbuilds.okbuck.rule.JavaTestRule

final class JavaTestRuleComposer {

    private JavaTestRuleComposer() {
        // no instance
    }

    static JavaTestRule compose(JavaLibTarget target) {
        List<String> deps = [
                ":src_${target.name}"
        ]

        deps.addAll(target.testCompileDeps.collect { String dep ->
            "//${dep.reverse().replaceFirst("/", ":").reverse()}"
        })

        deps.addAll(target.targetTestCompileDeps.collect { Target targetDep ->
            "//${targetDep.path}:src_${targetDep.name}"
        })

        Set<String> aptDeps = [] as Set
        aptDeps.addAll(target.aptDeps.collect { String dep ->
            "//${dep.reverse().replaceFirst("/", ":").reverse()}"
        })

        aptDeps.addAll(target.targetAptDeps.collect { Target targetDep ->
            "//${targetDep.path}:src_${targetDep.name}"
        })

        List<String> postprocessClassesCommands = []
        if (target.retrolambda) {
            postprocessClassesCommands.add(RetroLambdaGenerator.generate(target))
        }

        new JavaTestRule("src_${target.testName}", ["PUBLIC"], deps, target.testSources,
                target.annotationProcessors, aptDeps, target.sourceCompatibility,
                target.targetCompatibility, postprocessClassesCommands, target.jvmArgs)
    }
}
