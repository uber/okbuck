package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidLibTarget
import com.uber.okbuck.core.model.android.AndroidTarget
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.core.model.base.Target
import com.uber.okbuck.core.util.RetrolambdaUtil
import com.uber.okbuck.rule.android.AndroidLibraryRule

final class AndroidLibraryRuleComposer extends AndroidBuckRuleComposer {

    private AndroidLibraryRuleComposer() {
        // no instance
    }

    static AndroidLibraryRule compose(
            AndroidLibTarget target,
            List<String> deps,
            List<String> aptDeps,
            List<String> aidlRuleNames,
            String appClass) {
        List<String> libraryDeps = new ArrayList<>(deps)
        List<String> libraryAptDeps = new ArrayList<>(aptDeps)
        List<String> libraryAidlRuleNames = new ArrayList<>(aidlRuleNames)

        libraryDeps.addAll(external(target.main.externalDeps))
        libraryDeps.addAll(targets(target.main.targetDeps))

        Set<String> providedDeps = []
        providedDeps.addAll(external(target.provided.externalDeps))
        providedDeps.addAll(targets(target.provided.targetDeps))
        providedDeps.removeAll(libraryDeps)

        if (target.retrolambda) {
            providedDeps.add(RetrolambdaUtil.getRtStubJarRule())
        }

        target.main.targetDeps.each { Target targetDep ->
            if (targetDep instanceof AndroidTarget) {
                targetDep.resources.each { AndroidTarget.ResBundle bundle ->
                    libraryDeps.add(res(targetDep as AndroidTarget, bundle))
                }
            }
        }

        List<String> testTargets = []
        if (target.robolectric && target.test.sources) {
            testTargets.add(":${test(target)}")
        }

        return new AndroidLibraryRule(
                src(target),
                ["PUBLIC"],
                libraryDeps,
                target.main.sources,
                target.manifest,
                target.annotationProcessors as List,
                libraryAptDeps,
                providedDeps,
                libraryAidlRuleNames,
                appClass,
                target.sourceCompatibility,
                target.targetCompatibility,
                target.postprocessClassesCommands,
                target.main.jvmArgs,
                target.generateR2,
                testTargets,
                target.getExtraOpts(RuleType.ANDROID_LIBRARY))
    }
}
