package com.uber.okbuck.rule.android

import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.core.model.jvm.TestOptions

final class AndroidTestRule extends AndroidRule {

    private static final List<String> ANDROID_TEST_LABELS = ['unit', 'android', 'robolectric']

    AndroidTestRule(
            RuleType ruleType,
            String name,
            List<String> visibility,
            List<String> deps,
            Set<String> srcSet,
            String manifest,
            List<String> annotationProcessors,
            List<String> aptDeps,
            Set<String> providedDeps,
            List<String> aidlRuleNames,
            String appClass,
            String sourceCompatibility,
            String targetCompatibility,
            List<String> postprocessClassesCommands,
            List<String> options,
            TestOptions testOptions,
            String mResourcesDir,
            String runtimeDependency,
            Set<String> extraOpts) {

        super(
                ruleType,
                name,
                visibility,
                deps,
                srcSet,
                null,
                manifest,
                annotationProcessors,
                aptDeps,
                providedDeps,
                aidlRuleNames,
                appClass,
                sourceCompatibility,
                targetCompatibility,
                postprocessClassesCommands,
                options,
                testOptions,
                false,
                mResourcesDir,
                runtimeDependency,
                null,
                ANDROID_TEST_LABELS,
                extraOpts)
    }
}
