package com.uber.okbuck.composer

import com.uber.okbuck.core.model.AndroidLibTarget
import com.uber.okbuck.core.model.AndroidTarget
import com.uber.okbuck.core.model.Target
import com.uber.okbuck.core.util.RetrolambdaUtil
import com.uber.okbuck.rule.AndroidLibraryRule

final class AndroidLibraryRuleComposer extends AndroidBuckRuleComposer {

    private AndroidLibraryRuleComposer() {
        // no instance
    }

    static AndroidLibraryRule compose(
            AndroidLibTarget target,
            List<String> deps,
            List<String> aptDeps,
            List<String> aidlRuleNames,
            String appClass,
            Set<String> srcTargets = []) {

        List<String> libraryDeps = new ArrayList<>(deps);
        List<String> libraryAptDeps = new ArrayList<>(aptDeps);
        List<String> libraryAidlRuleNames = new ArrayList<>(aidlRuleNames);
        Set<String> providedDeps = []

        libraryDeps.addAll(external(target.main.externalDeps))
        libraryDeps.addAll(targets(target.main.targetDeps))

        providedDeps.addAll(external(target.apt.externalDeps))
        providedDeps.addAll(targets(target.apt.targetDeps))
        providedDeps.removeAll(libraryDeps)

        target.main.targetDeps.each { Target targetDep ->
            if (targetDep instanceof AndroidTarget) {
                targetDep.resources.each { AndroidTarget.ResBundle bundle ->
                    libraryDeps.add(res(targetDep as AndroidTarget, bundle))
                }
            }
        }

        String javac = null
        if (target.retrolambda && !target.main.sources.empty) {
            providedDeps.add(RetrolambdaUtil.getRtStubJarRule())
            javac = RetrolambdaUtil.PROJECT_RETROLAMBDAC
        }

        List<String> testTargets = [];
        if (target.robolectric && target.test.sources) {
            testTargets.add(":${test(target)}")
        }

        return new AndroidLibraryRule(
                src(target),
                ["PUBLIC"],
                libraryDeps,
                srcTargets,
                target.main.sources,
                target.manifest,
                target.annotationProcessors as List,
                libraryAptDeps,
                providedDeps,
                libraryAidlRuleNames,
                appClass,
                target.sourceCompatibility,
                target.targetCompatibility,
                javac,
                target.main.jvmArgs,
                target.generateR2,
                testTargets)
    }
}
