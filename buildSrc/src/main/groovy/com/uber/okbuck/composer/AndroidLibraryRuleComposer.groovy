package com.uber.okbuck.composer

import com.uber.okbuck.core.model.AndroidLibTarget
import com.uber.okbuck.core.model.AndroidTarget
import com.uber.okbuck.core.model.Target
import com.uber.okbuck.generator.RetroLambdaGenerator
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
            List<String> postProcessCommands,
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

        List<String> postprocessClassesCommands = []
        if (target.retrolambda) {
            postprocessClassesCommands.add(RetroLambdaGenerator.generate(target))
        }
        postprocessClassesCommands.addAll(postProcessCommands);

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
                testTargets,
                target.bootClasspath,
                target.rootProject.file("buck-out/gen").absolutePath)
    }
}
