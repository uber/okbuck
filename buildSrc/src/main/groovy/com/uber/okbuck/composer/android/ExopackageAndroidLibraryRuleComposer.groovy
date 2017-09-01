package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.template.android.AndroidRule
import com.uber.okbuck.template.core.Rule

final class ExopackageAndroidLibraryRuleComposer extends AndroidBuckRuleComposer {

    private ExopackageAndroidLibraryRuleComposer() {
        // no instance
    }

    static Rule compose(AndroidAppTarget target) {
        List<String> deps = []
        deps.addAll(external(target.exopackage.externalDeps))
        deps.addAll(targets(target.exopackage.targetDeps))
        deps.add(":${buildConfig(target)}")

        AndroidRule androidRule = new AndroidRule()
                .sourceCompatibility(target.sourceCompatibility)
                .targetCompatibility(target.targetCompatibility)
                .postprocessClassesCommands(target.postprocessClassesCommands)
                .options(target.main.jvmArgs)

        if (target.ruleType == RuleType.KOTLIN_ANDROID_LIBRARY) {
            androidRule = androidRule
                    .language("kotlin")
                    .extraKotlincArgs(target.kotlincArguments)
        }

        Set<String> extraBuckOpts = new HashSet<>(target.getExtraOpts(RuleType.ANDROID_LIBRARY))
        extraBuckOpts.add("srcs = ['${target.exopackage.appClass}']")

        return androidRule
                .ruleType(target.ruleType.buckName)
                .defaultVisibility()
                .deps(deps)
                .name(appLib(target))
                .extraBuckOpts(extraBuckOpts)
    }
}
