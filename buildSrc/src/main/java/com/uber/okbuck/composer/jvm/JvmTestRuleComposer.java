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

public final class JvmTestRuleComposer extends JvmBuckRuleComposer {

  private static final ImmutableSet<String> JAVA_TEST_LABELS = ImmutableSet.of("unit", "java");

  private JvmTestRuleComposer() {
    // no instance
  }

  public static Rule compose(JvmTarget target, RuleType ruleType) {
    List<String> deps =
        ImmutableList.<String>builder()
            .add(":" + src(target))
            .addAll(external(target.getExternalDeps(SourceSetType.TEST)))
            .addAll(targets(target.getTargetDeps(SourceSetType.TEST)))
            .build();

    Set<String> aptDeps =
        ImmutableSet.<String>builder()
            .addAll(external(target.getExternalAptDeps(SourceSetType.TEST)))
            .addAll(targets(target.getTargetAptDeps(SourceSetType.TEST)))
            .build();

    Set<String> providedDeps =
        ImmutableSet.<String>builder()
            .addAll(external(target.getExternalProvidedDeps(SourceSetType.TEST)))
            .addAll(targets(target.getTargetProvidedDeps(SourceSetType.TEST)))
            .build();

    return new JvmRule()
        .srcs(target.getTest().getSources())
        .exts(ruleType.getProperties())
        .apPlugins(getApPlugins(target.getTestApPlugins()))
        .aptDeps(aptDeps)
        .providedDeps(providedDeps)
        .resources(target.getTest().getJavaResources())
        .sourceCompatibility(target.getSourceCompatibility())
        .targetCompatibility(target.getTargetCompatibility())
        .options(target.getTest().getCustomOptions())
        .jvmArgs(target.getTestOptions().getJvmArgs())
        .env(target.getTestOptions().getEnv())
        .ruleType(ruleType.getBuckName())
        .defaultVisibility()
        .deps(deps)
        .name(test(target))
        .labels(JAVA_TEST_LABELS)
        .extraBuckOpts(target.getExtraOpts(ruleType));
  }
}
