package com.uber.okbuck.composer

import com.uber.okbuck.core.model.JavaLibTarget
import com.uber.okbuck.generator.RetroLambdaGenerator
import com.uber.okbuck.rule.JavaTestRule
import com.uber.okbuck.block.PostProcessClassessCommands

final class JavaTestRuleComposer extends JavaBuckRuleComposer {

    private JavaTestRuleComposer() {
        // no instance
    }

    static JavaTestRule compose(JavaLibTarget target, List<String> postProcessCommands) {
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

        PostProcessClassessCommands postprocessClassesCommands = new PostProcessClassessCommands(
                target.bootClasspath,
                target.rootProject.file("buck-out/gen").absolutePath);
        if (target.retrolambda) {
            postprocessClassesCommands.addCommand(RetroLambdaGenerator.generate(target))
        }
        postprocessClassesCommands.addCommands(postProcessCommands);

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
                target.test.jvmArgs)
    }
}
