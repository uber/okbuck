package com.uber.okbuck.rule.kotlin;

import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.rule.java.JavaLibraryRule;

import java.util.List;
import java.util.Set;

public final class KotlinLibraryRule extends JavaLibraryRule {

    public KotlinLibraryRule(
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
                testTargets,
                extraOpts,
                RuleType.KOTLIN_LIBRARY);
    }
}
