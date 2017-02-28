package com.uber.okbuck.rule.groovy;

import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.core.model.jvm.TestOptions;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class GroovyTestRule extends GroovyRule {

    private static final List<String> GROOVY_TEST_LABELS = Arrays.asList("unit", "groovy");

    public GroovyTestRule(
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
            TestOptions testOptions,
            Set<String> extraOpts) {
        super(RuleType.GROOVY_TEST,
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
                testOptions,
                GROOVY_TEST_LABELS,
                extraOpts);
    }
}
