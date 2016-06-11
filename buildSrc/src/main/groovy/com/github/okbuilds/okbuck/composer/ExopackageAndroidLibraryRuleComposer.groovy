package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.AndroidAppTarget
import com.github.okbuilds.core.model.Target
import com.github.okbuilds.okbuck.generator.RetroLambdaGenerator
import com.github.okbuilds.okbuck.rule.ExopackageAndroidLibraryRule
import org.apache.commons.lang3.tuple.Pair

final class ExopackageAndroidLibraryRuleComposer {

    private ExopackageAndroidLibraryRuleComposer() {
        // no instance
    }

    static ExopackageAndroidLibraryRule compose(AndroidAppTarget target) {

        List<String> deps = []
        Pair<Set<String>, Set<Target>> appLibDependencies = target.appLibDependencies

        deps.addAll(appLibDependencies.left.collect { String dep ->
            "//${dep.reverse().replaceFirst("/", ":").reverse()}"
        })

        deps.addAll(appLibDependencies.right.collect { Target depTarget ->
            "//${depTarget.path}:src_${depTarget.name}"
        })

        deps.add(":build_config_${target.name}")

        List<String> postprocessClassesCommands = []
        if (target.retrolambda) {
            postprocessClassesCommands.add(RetroLambdaGenerator.generate(target))
        }

        return new ExopackageAndroidLibraryRule("app_lib_${target.name}", target.appClass,
                ["PUBLIC"], deps, target.sourceCompatibility, target.targetCompatibility,
                postprocessClassesCommands, target.jvmArgs)
    }
}
