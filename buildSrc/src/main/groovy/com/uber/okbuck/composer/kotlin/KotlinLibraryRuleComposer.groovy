package com.uber.okbuck.composer.kotlin

import com.uber.okbuck.composer.jvm.JvmBuckRuleComposer
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.core.model.kotlin.KotlinLibTarget
import com.uber.okbuck.rule.java.JavaLibraryRule

final class KotlinLibraryRuleComposer extends JvmBuckRuleComposer {

    private KotlinLibraryRuleComposer() {
        // no instance
    }

    static JavaLibraryRule compose(KotlinLibTarget target) {
        List<String> deps = []
        deps.addAll(external(target.main.externalDeps))
        deps.addAll(targets(target.main.targetDeps))

        Set<String> providedDeps = []
        providedDeps.addAll(external(target.provided.externalDeps))
        providedDeps.addAll(targets(target.provided.targetDeps))
        providedDeps.removeAll(deps)

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
                target.getExtraOpts(RuleType.KOTLIN_LIBRARY),
                RuleType.KOTLIN_LIBRARY)
    }
}
