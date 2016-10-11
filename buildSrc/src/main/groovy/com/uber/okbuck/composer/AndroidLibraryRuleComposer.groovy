package com.uber.okbuck.composer

import com.uber.okbuck.constant.BuckConstants
import com.uber.okbuck.core.model.AndroidLibTarget
import com.uber.okbuck.core.model.AndroidTarget
import com.uber.okbuck.core.model.Target
import com.uber.okbuck.generator.RetroLambdaGenerator
import com.uber.okbuck.printable.PostProcessClassessCommands
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

        Set<String> postProcessDeps = []
        postProcessDeps.addAll(target.postProcess.externalDeps)

        target.main.targetDeps.each { Target targetDep ->
            if (targetDep instanceof AndroidTarget) {
                targetDep.resources.each { AndroidTarget.ResBundle bundle ->
                    libraryDeps.add(res(targetDep as AndroidTarget, bundle))
                }
            }
        }

        List<String> postProcessClassesCommands = []
        if (target.retrolambda) {
            postProcessClassesCommands.add(RetroLambdaGenerator.generate(target))
        }
        postProcessClassesCommands.addAll(target.postProcessClassesCommands)

        PostProcessClassessCommands postprocessClassesCommands = new PostProcessClassessCommands(
                target.bootClasspath,
                target.rootProject.file(BuckConstants.DEFAULT_BUCK_OUT_GEN_PATH).absolutePath,
                postProcessDeps,
                postProcessClassesCommands);

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
                postprocessClassesCommands,
                target.main.jvmArgs,
                target.generateR2,
                testTargets)
    }
}
