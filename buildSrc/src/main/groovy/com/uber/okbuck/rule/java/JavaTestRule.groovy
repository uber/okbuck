package com.uber.okbuck.rule.java

import com.uber.okbuck.core.model.base.RuleType

class JavaTestRule extends JavaRule {

    private static final List<String> JAVA_TEST_LABELS = ['unit', 'java']

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
            Set<String> extraOpts,
            RuleType ruleType = RuleType.JAVA_TEST,
            List<String> testLabels = JAVA_TEST_LABELS) {
        super(
                ruleType,
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
                testLabels,
                extraOpts)
    }
}
