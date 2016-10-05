package com.uber.okbuck.composer

import com.uber.okbuck.core.model.AndroidAppTarget
import com.uber.okbuck.generator.RetroLambdaGenerator
import com.uber.okbuck.rule.ExopackageAndroidLibraryRule

final class ExopackageAndroidLibraryRuleComposer extends AndroidBuckRuleComposer {

    private ExopackageAndroidLibraryRuleComposer() {
        // no instance
    }

    static ExopackageAndroidLibraryRule compose(AndroidAppTarget target, List<String> postProcessCommands) {
        List<String> deps = []

        deps.addAll(external(target.exopackage.externalDeps))
        deps.addAll(targets(target.exopackage.targetDeps))
        deps.add(":${buildConfig(target)}")

        List<String> postprocessClassesCommands = []
        if (target.retrolambda) {
            postprocessClassesCommands.add(RetroLambdaGenerator.generate(target))
        }
        postprocessClassesCommands.addAll(postProcessCommands);

        return new ExopackageAndroidLibraryRule(appLib(target), target.exopackage.appClass, ["PUBLIC"], deps,
                target.sourceCompatibility, target.targetCompatibility, postprocessClassesCommands,
                target.exopackage.jvmArgs)
    }
}
