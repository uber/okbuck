package com.uber.okbuck.composer.jvm;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.core.model.base.SourceSetType;
import com.uber.okbuck.core.model.jvm.JvmTarget;
import com.uber.okbuck.template.core.Rule;
import com.uber.okbuck.template.jvm.JvmRule;

import java.util.List;
import java.util.Set;

public final class JvmIntegrationTestRuleComposer extends JvmBuckRuleComposer {

  private static final ImmutableSet<String> JAVA_TEST_LABELS = ImmutableSet.of("integration", "java");

  private JvmIntegrationTestRuleComposer() {
    // no instance
  }

  public static Rule compose(JvmTarget target, RuleType ruleType) {
    Set<String> deps =
        ImmutableSet.<String>builder()
            .add(":" + src(target))
            .addAll(external(target.getExternalDeps(SourceSetType.INTEGRATION_TEST)))
            .addAll(external(target.getExternalDeps(SourceSetType.TEST)))
            .addAll(targets(target.getTargetDeps(SourceSetType.INTEGRATION_TEST)))
            .addAll(targets(target.getTargetDeps(SourceSetType.TEST)))
            .build();

    Set<String> aptDeps =
        ImmutableSet.<String>builder()
            .addAll(external(target.getExternalAptDeps(SourceSetType.INTEGRATION_TEST)))
            .addAll(external(target.getExternalAptDeps(SourceSetType.TEST)))
            .addAll(targets(target.getTargetAptDeps(SourceSetType.INTEGRATION_TEST)))
            .addAll(targets(target.getTargetAptDeps(SourceSetType.TEST)))
            .build();

    Set<String> providedDeps =
        ImmutableSet.<String>builder()
            .addAll(external(target.getExternalProvidedDeps(SourceSetType.INTEGRATION_TEST)))
            .addAll(external(target.getExternalProvidedDeps(SourceSetType.TEST)))
            .addAll(targets(target.getTargetProvidedDeps(SourceSetType.INTEGRATION_TEST)))
            .addAll(targets(target.getTargetProvidedDeps(SourceSetType.TEST)))
            .build();

    return new JvmRule()
        .srcs(target.getIntegrationTest().getSources())
        .exts(ruleType.getProperties())
        .apPlugins(getApPlugins(target.getIntegrationTestApPlugins()))
        .aptDeps(aptDeps)
        .providedDeps(providedDeps)
        .resources(target.getIntegrationTest().getJavaResources())
        .sourceCompatibility(target.getSourceCompatibility())
        .targetCompatibility(target.getTargetCompatibility())
        .options(target.getIntegrationTest().getCustomOptions())
        .jvmArgs(target.getIntegrationTestOptions().getJvmArgs())
        .env(target.getIntegrationTestOptions().getEnv())
        .ruleType(ruleType.getBuckName())
        .defaultVisibility()
        .deps(deps)
        .name(integrationTest(target))
        .labels(JAVA_TEST_LABELS)
        .extraBuckOpts(target.getExtraOpts(ruleType));
  }
}
