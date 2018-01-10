package com.uber.okbuck.composer.java

import com.google.common.collect.ImmutableSet
import com.uber.okbuck.composer.jvm.JvmBuckRuleComposer
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.core.model.java.JavaLibTarget
import com.uber.okbuck.template.core.Rule
import com.uber.okbuck.template.java.JavaRule

final class JavaTestRuleComposer extends JvmBuckRuleComposer {

    private static final Set<String> JAVA_TEST_LABELS = ImmutableSet.of('unit', 'java')

    private JavaTestRuleComposer() {
        // no instance
    }

    static Rule compose(JavaLibTarget target,
                        RuleType ruleType = RuleType.JAVA_TEST) {

        List<String> deps = []
        deps.add(":${src(target)}")
        deps.addAll(external(getExternalDeps(target.test, target.testProvided)))
        deps.addAll(targets(getTargetDeps(target.test, target.testProvided)))

        Set<String> aptDeps = [] as Set
        aptDeps.addAll(external(target.testApt.externalDeps))
        aptDeps.addAll(targets(target.testApt.targetDeps))

        Set<String> providedDeps = []
        providedDeps.addAll(external(getExternalProvidedDeps(target.test, target.testProvided)))
        providedDeps.addAll(targets(getTargetProvidedDeps(target.test, target.testProvided)))

        new JavaRule()
                .srcs(target.test.sources)
                .exts(ruleType.sourceExtensions)
                .annotationProcessors(target.testAnnotationProcessors)
                .aptDeps(aptDeps)
                .providedDeps(providedDeps)
                .resourcesDir(target.test.resourcesDir)
                .sourceCompatibility(target.sourceCompatibility)
                .targetCompatibility(target.targetCompatibility)
                .options(target.test.jvmArgs)
                .jvmArgs(target.testOptions.jvmArgs)
                .env(target.testOptions.env)
                .ruleType(ruleType.buckName)
                .defaultVisibility()
                .deps(deps)
                .name(test(target))
                .labels(JAVA_TEST_LABELS)
                .extraBuckOpts(target.getExtraOpts(ruleType))
    }
}
