package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.JavaLibTarget
import com.github.okbuilds.okbuck.generator.RetroLambdaGenerator
import com.github.okbuilds.okbuck.rule.JavaTestRule

final class JavaTestRuleComposer extends JavaBuckRuleComposer {

    private JavaTestRuleComposer() {
        // no instance
    }

    static JavaTestRule compose(JavaLibTarget target) {
        List<String> deps = [":${src(target)}"]
        deps.addAll(external(target.test.externalDeps))
        deps.addAll(targets(target.test.targetDeps))

        Set<String> aptDeps = [] as Set
        aptDeps.addAll(external(target.apt.externalDeps))
        aptDeps.addAll(targets(target.apt.targetDeps))

        List<String> postprocessClassesCommands = []
        if (target.retrolambda) {
            postprocessClassesCommands.add(RetroLambdaGenerator.generate(target))
        }

        new JavaTestRule(test(target), ["PUBLIC"], deps, target.test.sources,
                target.annotationProcessors, aptDeps, target.sourceCompatibility,
                target.targetCompatibility, postprocessClassesCommands, target.jvmArgs)
    }
}
