package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.JavaLibTarget
import com.github.okbuilds.core.model.Target
import com.github.okbuilds.okbuck.generator.RetroLambdaGenerator
import com.github.okbuilds.okbuck.rule.JavaLibraryRule

final class JavaLibraryRuleComposer {

    private JavaLibraryRuleComposer() {
        // no instance
    }

    static JavaLibraryRule compose(JavaLibTarget target) {
        List<String> deps = []

        deps.addAll(target.compileDeps.collect { String dep ->
            "//${dep.reverse().replaceFirst("/", ":").reverse()}"
        })

        deps.addAll(target.targetCompileDeps.collect { Target targetDep ->
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

        new JavaLibraryRule("src_${target.name}", ["PUBLIC"], deps, target.sources,
                target.annotationProcessors, aptDeps, target.sourceCompatibility,
                target.targetCompatibility, postprocessClassesCommands, target.jvmArgs)
    }
}
