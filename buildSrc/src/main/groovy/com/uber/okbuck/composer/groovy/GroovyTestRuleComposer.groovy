package com.uber.okbuck.composer.groovy

import com.uber.okbuck.composer.jvm.JvmBuckRuleComposer
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.core.model.groovy.GroovyLibTarget
import com.uber.okbuck.core.util.RetrolambdaUtil
import com.uber.okbuck.rule.java.JavaTestRule

final class GroovyTestRuleComposer extends JvmBuckRuleComposer {

    private GroovyTestRuleComposer() {
        // no instance
    }

    static JavaTestRule compose(GroovyLibTarget target) {
        List<String> deps = []
        deps.add(":${src(target)}")
        deps.addAll(external(target.test.externalDeps))
        deps.addAll(targets(target.test.targetDeps))

        Set<String> providedDeps = []
        providedDeps.addAll(external(target.testProvided.externalDeps))
        providedDeps.addAll(targets(target.testProvided.targetDeps))
        providedDeps.removeAll(deps)

        if (target.retrolambda) {
            providedDeps.add(RetrolambdaUtil.getRtStubJarRule())
        }

        new JavaTestRule(
                test(target),
                ["PUBLIC"],
                deps,
                target.test.sources,
                Collections.emptySet(),
                Collections.emptySet(),
                providedDeps,
                target.test.resourcesDir,
                target.sourceCompatibility,
                target.targetCompatibility,
                Collections.emptyList(),
                target.test.jvmArgs,
                target.testOptions,
                target.getExtraOpts(RuleType.GROOVY_TEST),
                RuleType.GROOVY_TEST,
                Arrays.asList("unit", "groovy"))
    }
}
