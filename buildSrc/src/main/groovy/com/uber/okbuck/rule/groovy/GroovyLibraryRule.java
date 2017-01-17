package com.uber.okbuck.rule.groovy;

import com.uber.okbuck.core.model.base.RuleType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class GroovyLibraryRule extends GroovyRule {

    public GroovyLibraryRule(
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
            Set<String> extraOpts) {
        super(RuleType.GROOVY_LIBRARY,
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
                Collections.emptyList(),
                Collections.emptyList(),
                extraOpts);
    }
}
