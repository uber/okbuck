package com.uber.okbuck.composer

import com.uber.okbuck.constant.BuckConstants
import com.uber.okbuck.core.model.AndroidAppTarget
import com.uber.okbuck.generator.RetroLambdaGenerator
import com.uber.okbuck.rule.ExopackageAndroidLibraryRule
import com.uber.okbuck.printable.PostProcessClassessCommands

final class ExopackageAndroidLibraryRuleComposer extends AndroidBuckRuleComposer {

    private ExopackageAndroidLibraryRuleComposer() {
        // no instance
    }

    static ExopackageAndroidLibraryRule compose(AndroidAppTarget target, List<String> postProcessCommands) {
        List<String> deps = []

        deps.addAll(external(target.exopackage.externalDeps))
        deps.addAll(targets(target.exopackage.targetDeps))
        deps.add(":${buildConfig(target)}")

        Set<String> postProcessDeps = []
        postProcessDeps.addAll(external(target.postProcess.externalDeps))
        postProcessDeps.addAll(targets(target.postProcess.targetDeps))

        PostProcessClassessCommands postprocessClassesCommands = new PostProcessClassessCommands(
                target.bootClasspath,
                target.rootProject.file(BuckConstants.DEFAULT_BUCK_OUT_GEN_PATH).absolutePath,
                postProcessDeps);
        if (target.retrolambda) {
            postprocessClassesCommands.addCommand(RetroLambdaGenerator.generate(target))
        }
        postprocessClassesCommands.addCommands(postProcessCommands);

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
