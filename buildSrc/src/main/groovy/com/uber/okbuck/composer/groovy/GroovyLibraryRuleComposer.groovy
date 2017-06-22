package com.uber.okbuck.composer.groovy

import com.uber.okbuck.composer.jvm.JvmBuckRuleComposer
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.core.model.groovy.GroovyLibTarget
import com.uber.okbuck.core.util.RetrolambdaUtil
import com.uber.okbuck.rule.java.JavaLibraryRule

final class GroovyLibraryRuleComposer extends JvmBuckRuleComposer {

    private GroovyLibraryRuleComposer() {
        // no instance
    }

    static JavaLibraryRule compose(GroovyLibTarget target) {
        List<String> deps = []
        deps.addAll(external(target.main.externalDeps))
        deps.addAll(targets(target.main.targetDeps))

        Set<String> providedDeps = []
        providedDeps.addAll(external(target.provided.externalDeps))
        providedDeps.addAll(targets(target.provided.targetDeps))
        providedDeps.removeAll(deps)

        if (target.retrolambda) {
            providedDeps.add(RetrolambdaUtil.getRtStubJarRule())
        }

        List<String> testTargets = []
        if (target.test.sources) {
            testTargets.add(":${test(target)}")
        }

        new JavaLibraryRule(
                src(target),
                ["PUBLIC"],
                deps,
                target.main.sources,
                Collections.emptySet(),
                Collections.emptySet(),
                providedDeps,
                target.main.resourcesDir,
                target.sourceCompatibility,
                target.targetCompatibility,
                Collections.emptyList(),
                target.main.jvmArgs,
                testTargets,
                target.getExtraOpts(RuleType.GROOVY_LIBRARY),
                RuleType.GROOVY_LIBRARY)
    }
}
