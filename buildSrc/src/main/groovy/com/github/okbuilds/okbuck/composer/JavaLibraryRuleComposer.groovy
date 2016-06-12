package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.JavaLibTarget
import com.github.okbuilds.okbuck.generator.RetroLambdaGenerator
import com.github.okbuilds.okbuck.rule.JavaLibraryRule

final class JavaLibraryRuleComposer extends JavaBuckRuleComposer {

    private JavaLibraryRuleComposer() {
        // no instance
    }

    static JavaLibraryRule compose(JavaLibTarget target) {
        List<String> deps = []
        deps.addAll(external(target.compileDeps))
        deps.addAll(targets(target.targetCompileDeps))

        Set<String> aptDeps = [] as Set
        aptDeps.addAll(external(target.aptDeps))
        aptDeps.addAll(targets(target.targetAptDeps))

        List<String> postprocessClassesCommands = []
        if (target.retrolambda) {
            postprocessClassesCommands.add(RetroLambdaGenerator.generate(target))
        }

        new JavaLibraryRule(src(target), ["PUBLIC"], deps, target.sources,
                target.annotationProcessors, aptDeps, target.sourceCompatibility,
                target.targetCompatibility, postprocessClassesCommands, target.jvmArgs)
    }
}
