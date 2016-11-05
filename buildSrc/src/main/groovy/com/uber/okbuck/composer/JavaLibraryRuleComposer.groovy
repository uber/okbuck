package com.uber.okbuck.composer

import com.uber.okbuck.core.model.JavaLibTarget
import com.uber.okbuck.generator.RetroLambdaGenerator
import com.uber.okbuck.printable.PostProcessClassessCommands
import com.uber.okbuck.rule.JavaLibraryRule

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

        Set<String> postProcessDeps = []
        postProcessDeps.addAll(target.postProcess.externalDeps)

        List<String> postProcessClassesCommands = []
        if (target.retrolambda) {
            postProcessClassesCommands.add(RetroLambdaGenerator.generate(target))
        }
        postProcessClassesCommands.addAll(target.postProcessClassesCommands)

        PostProcessClassessCommands postprocessClassesCommands = new PostProcessClassessCommands(
                target,
                postProcessDeps,
                postProcessClassesCommands);

        List<String> testTargets = [];
        if (target.test.sources) {
            testTargets.add(":${test(target)}")
        }

        new JavaLibraryRule(
                src(target),
                ["PUBLIC"],
                deps,
                target.main.sources,
                target.annotationProcessors,
                aptDeps,
                providedDeps,
                target.main.resourcesDir,
                target.sourceCompatibility,
                target.targetCompatibility,
                postprocessClassesCommands,
                target.main.jvmArgs,
                testTargets)
    }

}
