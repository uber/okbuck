package com.uber.okbuck.composer.android

import com.google.common.collect.ImmutableSet
import com.uber.okbuck.core.model.android.AndroidLibTarget
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.core.util.D8Util
import com.uber.okbuck.core.util.RobolectricUtil
import com.uber.okbuck.template.android.AndroidRule
import com.uber.okbuck.template.android.RobolectricRule
import com.uber.okbuck.template.core.Rule

final class AndroidTestRuleComposer extends AndroidBuckRuleComposer {

    private static final Set<String> ANDROID_TEST_LABELS = ImmutableSet.of('unit', 'android', 'robolectric')

    private AndroidTestRuleComposer() {
        // no instance
    }

    static Rule compose(
            AndroidLibTarget target,
            List<String> deps,
            final List<String> aidlRuleNames,
            String appClass) {

        List<String> testDeps = new ArrayList<>(deps)
        testDeps.add(":${src(target)}")
        testDeps.addAll(external(getExternalDeps(target.test, target.testProvided)))
        testDeps.addAll(targets(getTargetDeps(target.test, target.testProvided)))

        List<String> testAptDeps = []
        testAptDeps.addAll(external(target.testApt.externalDeps))
        testAptDeps.addAll(targets(target.testApt.targetDeps))

        Set<String> providedDeps = []
        providedDeps.addAll(external(getExternalProvidedDeps(target.test, target.testProvided)))
        providedDeps.addAll(targets(getTargetProvidedDeps(target.test, target.testProvided)))
        providedDeps.add(D8Util.RT_STUB_JAR_RULE)

        RobolectricRule robolectric = new RobolectricRule()
                .srcs(target.test.sources)
                .exts(target.testRuleType.sourceExtensions)
                .annotationProcessors(target.testAnnotationProcessors)
                .aptDeps(testAptDeps)
                .providedDeps(providedDeps)
                .resources(target.test.javaResources)
                .sourceCompatibility(target.sourceCompatibility)
                .targetCompatibility(target.targetCompatibility)
                .exportedDeps(aidlRuleNames)
                .excludes(appClass != null ? ImmutableSet.of(appClass) : ImmutableSet.of())
                .options(target.main.jvmArgs)
                .jvmArgs(target.testOptions.jvmArgs)
                .env(target.testOptions.env)
                .robolectricManifest(fileRule(target.manifest))
                .runtimeDependency(RobolectricUtil.ROBOLECTRIC_CACHE)

        if (target.testRuleType == RuleType.KOTLIN_ROBOLECTRIC_TEST) {
            robolectric = robolectric
                    .language("kotlin")
                    .extraKotlincArgs(target.kotlincArguments)
        }

        return robolectric
                .ruleType(target.testRuleType.buckName)
                .defaultVisibility()
                .deps(testDeps)
                .name(test(target))
                .labels(ANDROID_TEST_LABELS)
                .extraBuckOpts(target.getExtraOpts(RuleType.ROBOLECTRIC_TEST))
    }
}
