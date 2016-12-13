package com.uber.okbuck.composer.groovy

import com.uber.okbuck.composer.jvm.JvmBuckRuleComposer
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.core.model.groovy.GroovyLibTarget
import com.uber.okbuck.core.util.RetrolambdaUtil
import com.uber.okbuck.rule.groovy.GroovyLibraryRule

final class GroovyLibraryRuleComposer extends JvmBuckRuleComposer {

    private GroovyLibraryRuleComposer() {
        // no instance
    }

    static GroovyLibraryRule compose(GroovyLibTarget target) {
        List<String> deps = []
        deps.addAll(external(target.main.externalDeps))
        deps.addAll(targets(target.main.targetDeps))

        Set<String> providedDeps = []
        providedDeps.addAll(external(target.apt.externalDeps))
        providedDeps.addAll(targets(target.apt.targetDeps))
        providedDeps.removeAll(deps)

        if (target.retrolambda) {
            providedDeps.add(RetrolambdaUtil.getRtStubJarRule())
        }

        new GroovyLibraryRule(
                src(target),
                ["PUBLIC"],
                deps,
                target.main.sources,
                [],
                [],
                providedDeps,
                target.main.resourcesDir,
                target.sourceCompatibility,
                target.targetCompatibility,
                target.main.jvmArgs,
                target.getExtraOpts(RuleType.GROOVY_LIBRARY))
    }
}
