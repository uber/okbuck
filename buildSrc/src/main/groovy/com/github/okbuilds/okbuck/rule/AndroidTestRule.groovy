package com.github.okbuilds.okbuck.rule


final class AndroidTestRule extends AndroidRule {

    /**
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
            String mResourcesDir,
            String runtimeDependency,
            List<String> srcTargets) {

        super(
                "robolectric_test",
                name,
                visibility,
                deps,
                srcSet,
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
                false,
                mResourcesDir,
                runtimeDependency,
                null,
                srcTargets);
    }
}
