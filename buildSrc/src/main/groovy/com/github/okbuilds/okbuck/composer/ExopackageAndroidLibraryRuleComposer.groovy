package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.AndroidAppTarget
import com.github.okbuilds.core.model.Target
import com.github.okbuilds.okbuck.generator.RetroLambdaGenerator
import com.github.okbuilds.okbuck.rule.ExopackageAndroidLibraryRule
import org.apache.commons.lang3.tuple.Pair

final class ExopackageAndroidLibraryRuleComposer extends AndroidBuckRuleComposer {

    private ExopackageAndroidLibraryRuleComposer() {
        // no instance
    }

    static ExopackageAndroidLibraryRule compose(AndroidAppTarget target) {
        List<String> deps = []
        Pair<Set<String>, Set<Target>> appLibDependencies = target.appLibDependencies

        deps.addAll(external(appLibDependencies.left))
        deps.addAll(targets(appLibDependencies.right))
        deps.add(":${buildConfig(target)}")

        List<String> postprocessClassesCommands = []
        if (target.retrolambda) {
            postprocessClassesCommands.add(RetroLambdaGenerator.generate(target))
        }

        return new ExopackageAndroidLibraryRule(appLib(target), target.appClass, ["PUBLIC"], deps,
                target.sourceCompatibility, target.targetCompatibility, postprocessClassesCommands,
                target.jvmArgs)
    }
}
