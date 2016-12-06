package com.uber.okbuck.rule.java

import com.uber.okbuck.core.model.base.RuleType

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
            List<String> postprocessClassesCommands,
            List<String> options,
            List<String> testRunnerJvmArgs,
            Set<String> extraOpts) {
        super(
                RuleType.JAVA_TEST,
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
                testRunnerJvmArgs,
                null,
                ['unit', 'java'],
                extraOpts)
    }
}
