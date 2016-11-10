package com.uber.okbuck.composer

import com.uber.okbuck.core.model.JavaLibTarget
import com.uber.okbuck.generator.RetroLambdaGenerator
import com.uber.okbuck.printable.PostProcessClassessCommands
import com.uber.okbuck.rule.JavaTestRule

final class JavaTestRuleComposer extends JavaBuckRuleComposer {

    private JavaTestRuleComposer() {
        // no instance
    }

    static JavaTestRule compose(JavaLibTarget target) {
        List<String> deps = []
        deps.add(":${src(target)}")
        deps.addAll(external(target.test.externalDeps))
        deps.addAll(targets(target.test.targetDeps))

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

        new JavaTestRule(
                test(target),
                ["PUBLIC"],
                deps,
                target.test.sources,
                target.annotationProcessors,
                aptDeps,
                providedDeps,
                target.test.resourcesDir,
                target.sourceCompatibility,
                target.targetCompatibility,
                postprocessClassesCommands,
                target.test.jvmArgs,
                target.testRunnerJvmArgs)
    }
}
