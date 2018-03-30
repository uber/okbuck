package com.uber.okbuck.composer.java

import com.google.common.collect.ImmutableSet
import com.uber.okbuck.composer.jvm.JvmBuckRuleComposer
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.core.model.java.JavaLibTarget
import com.uber.okbuck.template.core.Rule
import com.uber.okbuck.template.java.JavaBinaryRule
import com.uber.okbuck.template.java.JavaRule

final class JavaLibraryRuleComposer extends JvmBuckRuleComposer {

    private JavaLibraryRuleComposer() {
        // no instance
    }

    static List<Rule> compose(JavaLibTarget target,
                              RuleType ruleType = RuleType.JAVA_LIBRARY) {

        List<String> deps = []
        deps.addAll(external(getExternalDeps(target.main, target.provided)))
        deps.addAll(targets(getTargetDeps(target.main, target.provided)))

        Set<String> aptDeps = [] as Set
        aptDeps.addAll(externalApt(target.apt.externalDeps))
        aptDeps.addAll(targetsApt(target.apt.targetDeps))

        Set<String> providedDeps = []
        providedDeps.addAll(external(getExternalProvidedDeps(target.main, target.provided)))
        providedDeps.addAll(targets(getTargetProvidedDeps(target.main, target.provided)))

        List<String> testTargets = []
        if (target.test.sources) {
            testTargets.add(":${test(target)}")
        }

        List<Rule> rules = []
        rules.add(new JavaRule()
                .srcs(target.main.sources)
                .exts(ruleType.sourceExtensions)
                .annotationProcessors(target.annotationProcessors)
                .aptDeps(aptDeps)
                .providedDeps(providedDeps)
                .resources(target.main.javaResources)
                .sourceCompatibility(target.sourceCompatibility)
                .targetCompatibility(target.targetCompatibility)
                .testTargets(testTargets)
                .options(target.main.jvmArgs)
                .ruleType(ruleType.buckName)
                .defaultVisibility()
                .deps(target.hasApplication() ? ImmutableSet.of() : deps)
                .libDeps(target.hasApplication() ? deps : ImmutableSet.of())
                .name(src(target))
                .extraBuckOpts(target.getExtraOpts(ruleType))
        )

        if (target.hasApplication()) {
            rules.add(new JavaBinaryRule()
                    .mainClassName(target.mainClass)
                    .excludes(target.excludes)
                    .defaultVisibility()
                    .name(bin(target))
                    .ruleType(RuleType.JAVA_BINARY.buckName)
                    .extraBuckOpts(target.getExtraOpts(RuleType.JAVA_BINARY))

            )
        }

        return rules
    }
}
