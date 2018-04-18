package com.uber.okbuck.composer.java;

import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.composer.jvm.JvmBuckRuleComposer;
import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.template.core.Rule;
import com.uber.okbuck.template.java.JavaAnnotationProcessorRule;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class JavaAnnotationProcessorRuleComposer extends JvmBuckRuleComposer {

    JavaAnnotationProcessorRuleComposer() { }

    /**
     * Uses the annotation processors scope list to generate the java_annotation_processor
     * rules. It sorts them based on the the annotation processor's UUID.
     *
     * @param scopeList List of annotation processor scopes.
     * @return A list containing java_annotation_processor rules.
     */
    public static List<Rule> compose(Collection<Scope> scopeList) {
        return scopeList
                .stream()
                .filter(scope -> !scope.getAnnotationProcessors().isEmpty())
                .sorted((scope1, scope2) ->
                        scope1.getAnnotationProcessorsUUID()
                                .compareToIgnoreCase(scope2.getAnnotationProcessorsUUID())
                )
                .map(scope -> {
                    ImmutableSet.Builder<String> depsBuilder = new ImmutableSet.Builder<>();
                    depsBuilder.addAll(externalApt(scope.getExternalDeps()));
                    depsBuilder.addAll(targetsApt(scope.getTargetDeps()));

                    return new JavaAnnotationProcessorRule()
                            .processorClasses(scope.getAnnotationProcessors())
                            .name(getApPluginRuleName(scope.getAnnotationProcessorsUUID()))
                            .deps(depsBuilder.build());
                })
                .collect(Collectors.toList());
    }
}