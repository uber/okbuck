package com.uber.okbuck.composer

import com.uber.okbuck.core.model.JavaLibTarget
import com.uber.okbuck.core.util.RetrolambdaUtil
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

        String javac = null
        if (target.retrolambda && !target.main.sources.empty) {
            providedDeps.add(RetrolambdaUtil.getRtStubJarRule())
            javac = RetrolambdaUtil.PROJECT_RETROLAMBDAC
        }

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
                javac,
                target.main.jvmArgs,
                testTargets)
    }

}
