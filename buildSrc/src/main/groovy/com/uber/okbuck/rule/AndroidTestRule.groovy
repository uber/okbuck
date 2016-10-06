package com.uber.okbuck.rule

import com.uber.okbuck.printable.PostProcessClassessCommands


final class AndroidTestRule extends AndroidRule {

    /**
     * @srcTargets, used for SqlDelight support(or other case), genrule's output will be used as src, pass empty set if not present
     * @param appClass , if exopackage is enabled, pass the detected app class, otherwise, pass null
     * */
    AndroidTestRule(
            String name,
            List<String> visibility,
            List<String> deps,
            Set<String> srcTargets,
            Set<String> srcSet,
            String manifest,
            List<String> annotationProcessors,
            List<String> aptDeps,
            Set<String> providedDeps,
            List<String> aidlRuleNames,
            String appClass,
            String sourceCompatibility,
            String targetCompatibility,
            PostProcessClassessCommands postprocessClassesCommands,
            List<String> options,
            String mResourcesDir,
            String runtimeDependency) {

        super(
                "robolectric_test",
                name,
                visibility,
                deps,
                srcTargets,
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
                null)
    }
}
