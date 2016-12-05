package com.uber.okbuck.rule.groovy

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
            List<String> testRunnerJvmArgs) {
        super(
                "groovy_test",
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
                ['unit', 'java'])
    }
}
