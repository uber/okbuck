package com.uber.okbuck.composer

import com.uber.okbuck.constant.BuckConstants
import com.uber.okbuck.core.model.AndroidAppTarget
import com.uber.okbuck.generator.RetroLambdaGenerator
import com.uber.okbuck.printable.PostProcessClassessCommands
import com.uber.okbuck.rule.ExopackageAndroidLibraryRule

final class ExopackageAndroidLibraryRuleComposer extends AndroidBuckRuleComposer {

    private ExopackageAndroidLibraryRuleComposer() {
        // no instance
    }

    static ExopackageAndroidLibraryRule compose(AndroidAppTarget target) {
        List<String> deps = []

        deps.addAll(external(target.exopackage.externalDeps))
        deps.addAll(targets(target.exopackage.targetDeps))
        deps.add(":${buildConfig(target)}")

        Set<String> postProcessDeps = []
        postProcessDeps.addAll(target.postProcess.externalDeps)

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

        return new ExopackageAndroidLibraryRule(
                appLib(target),
                target.exopackage.appClass,
                ["PUBLIC"],
                deps,
                target.sourceCompatibility,
                target.targetCompatibility,
                postprocessClassesCommands,
                target.exopackage.jvmArgs)
    }
}
