package com.uber.okbuck.composer.jvm;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.core.model.base.SourceSetType;
import com.uber.okbuck.core.model.jvm.JvmTarget;
import com.uber.okbuck.template.core.Rule;
import com.uber.okbuck.template.jvm.JvmBinaryRule;
import com.uber.okbuck.template.jvm.JvmRule;
import java.util.List;
import java.util.Set;

@SuppressWarnings("NullAway")
public final class JvmLibraryRuleComposer extends JvmBuckRuleComposer {

  private JvmLibraryRuleComposer() {
    // no instance
  }

  public static ImmutableList<Rule> compose(JvmTarget target, RuleType ruleType) {
    List<String> deps =
        ImmutableList.<String>builder()
            .addAll(external(target.getExternalDeps(SourceSetType.MAIN)))
            .addAll(targets(target.getTargetDeps(SourceSetType.MAIN)))
            .build();

    Set<String> aptDeps =
        ImmutableSet.<String>builder()
            .addAll(externalApt(target.getExternalAptDeps(SourceSetType.MAIN)))
            .addAll(targetsApt(target.getTargetAptDeps(SourceSetType.MAIN)))
            .build();

    Set<String> providedDeps =
        ImmutableSet.<String>builder()
            .addAll(external(target.getExternalProvidedDeps(SourceSetType.MAIN)))
            .addAll(targets(target.getTargetProvidedDeps(SourceSetType.MAIN)))
            .build();

    Set<String> exportedDeps =
        ImmutableSet.<String>builder()
            .addAll(external(target.getExternalExportedDeps(SourceSetType.MAIN)))
            .addAll(targets(target.getTargetExportedDeps(SourceSetType.MAIN)))
            .build();

    List<String> testTargets =
        !target.getTest().getSources().isEmpty()
            ? ImmutableList.of(":" + test(target))
            : ImmutableList.of();

    ImmutableList.Builder<Rule> rulesBuilder = new ImmutableList.Builder<>();
    rulesBuilder.add(
        new JvmRule()
            .srcs(target.getMain().getSources())
            .exts(ruleType.getProperties())
            .apPlugins(getApPlugins(target.getApPlugins()))
            .aptDeps(aptDeps)
            .providedDeps(providedDeps)
            .exportedDeps(exportedDeps)
            .resources(target.getMain().getJavaResources())
            .sourceCompatibility(target.getSourceCompatibility())
            .targetCompatibility(target.getTargetCompatibility())
            .mavenCoords(target.getMavenCoords())
            .testTargets(testTargets)
            .options(target.getMain().getCustomOptions())
            .ruleType(ruleType.getBuckName())
            .defaultVisibility()
            .deps(deps)
            .name(src(target))
            .extraBuckOpts(target.getExtraOpts(ruleType)));

    if (target.hasApplication()) {
      rulesBuilder.add(
          new JvmBinaryRule()
              .mainClassName(target.getMainClass())
              .excludes(target.getExcludes())
              .defaultVisibility()
              .name(bin(target))
              .deps(ImmutableSet.of(":" + src(target)))
              .ruleType(RuleType.JAVA_BINARY.getBuckName())
              .extraBuckOpts(target.getExtraOpts(RuleType.JAVA_BINARY)));
    }

    return rulesBuilder.build();
  }
}
