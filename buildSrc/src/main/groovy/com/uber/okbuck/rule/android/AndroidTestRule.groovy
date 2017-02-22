package com.uber.okbuck.rule.android

import com.uber.okbuck.core.model.base.RuleType

final class AndroidTestRule extends AndroidRule {

    private static final List<String> ANDROID_TEST_LABELS = ['unit', 'android', 'robolectric']

    AndroidTestRule(
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
            List<String> testRunnerJvmArgs,
            String mResourcesDir,
            String runtimeDependency,
            Set<String> extraOpts) {

        super(
                RuleType.ROBOLECTRIC_TEST,
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
                testRunnerJvmArgs,
                false,
                mResourcesDir,
                runtimeDependency,
                null,
                ANDROID_TEST_LABELS,
                extraOpts)
    }
}
