package com.uber.okbuck.composer.android

import com.google.common.collect.ImmutableSet
import com.uber.okbuck.core.model.android.AndroidLibTarget
import com.uber.okbuck.core.model.android.AndroidTarget
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.core.model.base.Target
import com.uber.okbuck.core.util.D8Util
import com.uber.okbuck.template.android.AndroidRule
import com.uber.okbuck.template.core.Rule

final class AndroidLibraryRuleComposer extends AndroidBuckRuleComposer {

    private AndroidLibraryRuleComposer() {
        // no instance
    }

    static Rule compose(
            AndroidLibTarget target,
            List<String> deps,
            final List<String> aidlRuleNames,
            String appClass,
            boolean useApPlugin) {

        Set<String> libraryDeps = new HashSet<>(deps)
        libraryDeps.addAll(external(getExternalDeps(target.main, target.provided)))
        libraryDeps.addAll(targets(getTargetDeps(target.main, target.provided)))

        List<String> libraryAptDeps = []
        libraryAptDeps.addAll(externalApt(target.apt.externalDeps))
        libraryAptDeps.addAll(targetsApt(target.apt.targetDeps))

        Set<String> providedDeps = new HashSet<>()
        providedDeps.addAll(external(getExternalProvidedDeps(target.main, target.provided)))
        providedDeps.addAll(targets(getTargetProvidedDeps(target.main, target.provided)))
        providedDeps.add(D8Util.RT_STUB_JAR_RULE)

        libraryDeps.addAll(getTargetDeps(target.main, target.provided).findAll { Target targetDep ->
            targetDep instanceof AndroidTarget
        }.collect { Target targetDep ->
            resRule(targetDep as AndroidTarget)
        })

        List<String> testTargets = []
        if (target.robolectricEnabled && target.test.sources) {
            testTargets.add(":${test(target)}")
        }
        if (target.libInstrumentationTarget && target.instrumentationTest.sources) {
            testTargets.add(":${bin(target.libInstrumentationTarget)}")
        }

        AndroidRule androidRule = new AndroidRule()
                .srcs(target.main.sources)
                .exts(target.ruleType.sourceExtensions)
                .manifest(fileRule(target.manifest))
                .proguardConfig(target.consumerProguardConfig)
                .annotationProcessors(target.annotationProcessors)
                .aptDeps(libraryAptDeps)
                .useAnnotationProcessorPlugin(useApPlugin)
                .providedDeps(providedDeps)
                .resources(target.main.javaResources)
                .sourceCompatibility(target.sourceCompatibility)
                .targetCompatibility(target.targetCompatibility)
                .testTargets(testTargets)
                .exportedDeps(aidlRuleNames)
                .excludes(appClass != null ? ImmutableSet.of(appClass) : ImmutableSet.of())
                .generateR2(target.generateR2)
                .options(target.main.jvmArgs)

        if (target.ruleType == RuleType.KOTLIN_ANDROID_LIBRARY) {
            androidRule = androidRule
                    .language("kotlin")
                    .extraKotlincArgs(target.kotlincArguments)
        }

        return androidRule
                .ruleType(target.ruleType.buckName)
                .defaultVisibility()
                .deps(libraryDeps)
                .name(src(target))
                .extraBuckOpts(target.getExtraOpts(RuleType.ANDROID_LIBRARY))
    }
}
