package com.uber.okbuck.composer

import com.uber.okbuck.core.model.AndroidAppTarget
import com.uber.okbuck.core.util.RetrolambdaUtil
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

        String javac = null
        if (target.retrolambda && !target.main.sources.empty) {
            javac = RetrolambdaUtil.PROJECT_RETROLAMBDAC
        }

        return new ExopackageAndroidLibraryRule(
                appLib(target),
                target.exopackage.appClass,
                ["PUBLIC"],
                deps,
                target.sourceCompatibility,
                target.targetCompatibility,
                javac,
                target.exopackage.jvmArgs)
    }
}
