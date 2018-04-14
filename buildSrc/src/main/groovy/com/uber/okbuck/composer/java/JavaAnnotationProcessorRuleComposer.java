package com.uber.okbuck.composer.java;

import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.template.core.Rule;
import com.uber.okbuck.template.java.JavaAnnotationProcessorRule;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class JavaAnnotationProcessorRuleComposer extends BuckRuleComposer {

    JavaAnnotationProcessorRuleComposer() { }

    public static List<Rule> compose(Collection<Scope> scopeList) {
        return scopeList
                .stream()
                .filter(scope -> !scope.getAnnotationProcessors().isEmpty())
                .sorted((scope1, scope2) ->
                        scope1.getAnnotationProcessorRuleName()
                                .compareToIgnoreCase(scope2.getAnnotationProcessorRuleName())
                )
                .map(scope -> {
                    ImmutableSet.Builder<String> depsBuilder = new ImmutableSet.Builder<>();
                    depsBuilder.addAll(externalApt(scope.getExternalDeps()));
                    depsBuilder.addAll(targetsApt(scope.getTargetDeps()));

                    return new JavaAnnotationProcessorRule()
                            .processorClasses(scope.getAnnotationProcessors())
                            .name(scope.getAnnotationProcessorRuleName())
                            .deps(depsBuilder.build());
                })
                .collect(Collectors.toList());
    }
}
