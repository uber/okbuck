package com.uber.okbuck.rule.kotlin;

import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.rule.java.JavaTestRule;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class KotlinTestRule extends JavaTestRule {

    private static final List<String> KOTLIN_TEST_LABELS = Arrays.asList("unit", "kotlin");

    public KotlinTestRule(
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
            List<String> testRunnerJvmArgs, Set<String> extraOpts) {
        super(name,
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
                extraOpts,
                RuleType.KOTLIN_TEST,
                KOTLIN_TEST_LABELS);
    }
}
