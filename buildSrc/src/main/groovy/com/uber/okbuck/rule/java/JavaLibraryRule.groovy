package com.uber.okbuck.rule.java

import com.uber.okbuck.core.model.base.RuleType

final class JavaLibraryRule extends JavaRule {

    JavaLibraryRule(
            String name,
            List<String> visibility,
            List<String> deps,
            Set<String> srcSet,
            Set<String> annotationProcessors,
            Set<String> aptDeps,
            Set<String> providedDeps,
            String resourcesDir,
            String sourceCompatibility,
            String targetCompatibility,
            List<String> postprocessClassesCommands,
            List<String> options,
            List<String> testTargets,
            Set<String> extraOpts) {

        super(
                RuleType.JAVA_LIBRARY,
                name,
                visibility,
                deps,
                srcSet,
                annotationProcessors,
                aptDeps,
                providedDeps,
                resourcesDir,
                sourceCompatibility,
                targetCompatibility,
                postprocessClassesCommands,
                options,
                null,
                testTargets,
                null,
                extraOpts)
    }
}
