package com.uber.okbuck.composer.android

import com.google.common.collect.ImmutableSet
import com.uber.okbuck.core.model.android.AndroidLibTarget
import com.uber.okbuck.core.model.android.AndroidTarget
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.core.model.base.Target
import com.uber.okbuck.core.util.RetrolambdaUtil
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
            String appClass) {
        List<String> libraryDeps = new ArrayList<>(deps)
        libraryDeps.addAll(external(target.main.externalDeps))
        libraryDeps.addAll(targets(target.main.targetDeps))

        List<String> libraryAptDeps = []
        libraryAptDeps.addAll(externalApt(target.apt.externalDeps))
        libraryAptDeps.addAll(targetsApt(target.apt.targetDeps))

        Set<String> providedDeps = []
        providedDeps.addAll(external(target.provided.externalDeps))
        providedDeps.addAll(targets(target.provided.targetDeps))
        providedDeps.removeAll(libraryDeps)

        if (target.retrolambda) {
            providedDeps.add(RetrolambdaUtil.getRtStubJarRule())
        }

        libraryDeps.addAll(target.main.targetDeps.findAll { Target targetDep ->
            targetDep instanceof AndroidTarget
        }.collect { Target targetDep ->
            resRule(targetDep as AndroidTarget)
        })

        List<String> testTargets = []
        if (target.robolectric && target.test.sources) {
            testTargets.add(":${test(target)}")
        }

        AndroidRule androidRule = new AndroidRule()
                .srcs(target.main.sources)
                .exts(target.ruleType.sourceExtensions)
                .manifest(fileRule(target.manifest))
                .annotationProcessors(target.annotationProcessors)
                .aptDeps(libraryAptDeps)
                .providedDeps(providedDeps)
                .resourcesDir(target.main.resourcesDir)
                .sourceCompatibility(target.sourceCompatibility)
                .targetCompatibility(target.targetCompatibility)
                .postprocessClassesCommands(target.postprocessClassesCommands)
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
