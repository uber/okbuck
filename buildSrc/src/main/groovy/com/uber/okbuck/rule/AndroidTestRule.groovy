package com.uber.okbuck.rule

final class AndroidTestRule extends AndroidRule {

    /**
     * @srcTargets , used for SqlDelight support(or other case), genrule's output will be used as src, pass empty set if not present
     * @param appClass , if exopackage is enabled, pass the detected app class, otherwise, pass null
     * */
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
            String runtimeDependency) {

        super(
                "robolectric_test",
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
                ['unit', 'android', 'robolectric'])
    }
}
