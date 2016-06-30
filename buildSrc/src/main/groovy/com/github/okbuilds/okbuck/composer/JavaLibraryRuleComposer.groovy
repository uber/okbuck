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
        deps.addAll(external(target.main.externalDeps))
        deps.addAll(targets(target.main.targetDeps))

        Set<String> aptDeps = [] as Set
        aptDeps.addAll(external(target.apt.externalDeps))
        aptDeps.addAll(targets(target.apt.targetDeps))

        Set<String> providedDeps = []
        providedDeps.addAll(external(target.apt.externalDeps))
        providedDeps.addAll(targets(target.apt.targetDeps))
        providedDeps.removeAll(deps)

        List<String> postprocessClassesCommands = []
        if (target.retrolambda) {
            postprocessClassesCommands.add(RetroLambdaGenerator.generate(target))
        }

        new JavaLibraryRule(src(target), ["PUBLIC"], deps, target.main.sources,
                target.annotationProcessors, aptDeps, providedDeps, target.sourceCompatibility,
                target.targetCompatibility, postprocessClassesCommands, target.jvmArgs)
    }
}
