package com.uber.okbuck.rule.groovy

import com.uber.okbuck.core.model.base.RuleType

final class GroovyTestRule extends GroovyRule {

    GroovyTestRule(
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
            List<String> javacOptions,
            List<String> testRunnerJvmArgs,
            Set<String> extraOpts) {
        super(
                RuleType.GROOVY_TEST,
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
                javacOptions,
                testRunnerJvmArgs,
                ['unit', 'java'],
                extraOpts)
    }
}
