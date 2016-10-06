package com.uber.okbuck.rule

import com.uber.okbuck.block.PostProcessClassessCommands

final class JavaTestRule extends JavaRule {

    JavaTestRule(
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
            PostProcessClassessCommands postprocessClassesCommands,
            List<String> options) {
        super(
                "java_test",
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
                null)
    }
}
