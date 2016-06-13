package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.AndroidAppTarget
import com.github.okbuilds.okbuck.generator.RetroLambdaGenerator
import com.github.okbuilds.okbuck.rule.ExopackageAndroidLibraryRule

final class ExopackageAndroidLibraryRuleComposer extends AndroidBuckRuleComposer {

    private ExopackageAndroidLibraryRuleComposer() {
        // no instance
    }

    static ExopackageAndroidLibraryRule compose(AndroidAppTarget target) {
        List<String> deps = []

        deps.addAll(external(target.exopackage.externalDeps))
        deps.addAll(targets(target.exopackage.targetDeps))
        deps.add(":${buildConfig(target)}")

        List<String> postprocessClassesCommands = []
        if (target.retrolambda) {
            postprocessClassesCommands.add(RetroLambdaGenerator.generate(target))
        }

        return new ExopackageAndroidLibraryRule(appLib(target), target.exopackage.appClass, ["PUBLIC"], deps,
                target.sourceCompatibility, target.targetCompatibility, postprocessClassesCommands,
                target.jvmArgs)
    }
}
