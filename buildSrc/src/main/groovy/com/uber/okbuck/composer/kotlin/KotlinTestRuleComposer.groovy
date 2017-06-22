package com.uber.okbuck.composer.kotlin

import com.uber.okbuck.composer.jvm.JvmBuckRuleComposer
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.core.model.kotlin.KotlinLibTarget
import com.uber.okbuck.rule.java.JavaTestRule

final class KotlinTestRuleComposer extends JvmBuckRuleComposer {

    private KotlinTestRuleComposer() {
        // no instance
    }

    static JavaTestRule compose(KotlinLibTarget target) {
        List<String> deps = []
        deps.add(":${src(target)}")
        deps.addAll(external(target.test.externalDeps))
        deps.addAll(targets(target.test.targetDeps))

        Set<String> providedDeps = []
        providedDeps.addAll(external(target.testProvided.externalDeps))
        providedDeps.addAll(targets(target.testProvided.targetDeps))
        providedDeps.removeAll(deps)

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
                target.getExtraOpts(RuleType.KOTLIN_TEST),
                RuleType.KOTLIN_TEST,
                Arrays.asList("unit", "kotlin"))
    }
}
