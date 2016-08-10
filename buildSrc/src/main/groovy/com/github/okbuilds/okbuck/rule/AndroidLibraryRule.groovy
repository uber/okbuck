package com.github.okbuilds.okbuck.rule


final class AndroidLibraryRule extends AndroidRule {

    /**
     * @srcTargets, used for SqlDelight support(or other case), genrule's output will be used as src, pass empty set if not present
     * @param appClass , if exopackage is enabled, pass the detected app class, otherwise, pass null
     * */
    AndroidLibraryRule(
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
            List<String> postprocessClassesCommands,
            List<String> options,
            boolean generateR2,
            List<String> testTargets) {

        super(
                "android_library",
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
                generateR2,
                null,
                null,
                testTargets,
                null)
    }
}
