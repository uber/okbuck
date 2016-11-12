package com.uber.okbuck.composer

import com.uber.okbuck.core.model.JavaLibTarget
import com.uber.okbuck.core.util.RetrolambdaUtil
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

        String javac = null
        if (target.retrolambda && !target.test.sources.empty) {
            providedDeps.add(RetrolambdaUtil.getRtStubJarRule())
            javac = RetrolambdaUtil.PROJECT_RETROLAMBDAC
        }

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
                javac,
                target.test.jvmArgs,
                target.testRunnerJvmArgs)
    }
}
