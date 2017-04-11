package com.uber.okbuck.composer.groovy

import com.uber.okbuck.composer.jvm.JvmBuckRuleComposer
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.core.model.groovy.GroovyLibTarget
import com.uber.okbuck.core.util.RetrolambdaUtil
import com.uber.okbuck.rule.groovy.GroovyTestRule

final class GroovyTestRuleComposer extends JvmBuckRuleComposer {

    private GroovyTestRuleComposer() {
        // no instance
    }

    static GroovyTestRule compose(GroovyLibTarget target) {
        List<String> deps = []
        deps.add(":${src(target)}")
        deps.addAll(external(target.test.externalDeps, target))
        deps.addAll(targets(target.test.targetDeps))

        Set<String> providedDeps = []
        providedDeps.addAll(external(target.testProvided.externalDeps, target))
        providedDeps.addAll(targets(target.testProvided.targetDeps))
        providedDeps.removeAll(deps)

        if (target.retrolambda) {
            providedDeps.add(RetrolambdaUtil.getRtStubJarRule(target))
        }

        new GroovyTestRule(
                test(target),
                ["PUBLIC"],
                deps,
                target.test.sources,
                [] as Set,
                [] as Set,
                providedDeps,
                target.test.resourcesDir,
                target.sourceCompatibility,
                target.targetCompatibility,
                target.test.jvmArgs,
                target.testOptions,
                target.getExtraOpts(RuleType.GROOVY_TEST))
    }
}
