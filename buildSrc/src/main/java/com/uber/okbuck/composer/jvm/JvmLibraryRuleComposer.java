package com.uber.okbuck.composer.jvm;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.core.model.jvm.JvmTarget;
import com.uber.okbuck.template.core.Rule;
import com.uber.okbuck.template.jvm.JvmBinaryRule;
import com.uber.okbuck.template.jvm.JvmRule;
import java.util.List;
import java.util.Set;

public final class JvmLibraryRuleComposer extends JvmBuckRuleComposer {

  private JvmLibraryRuleComposer() {
    // no instance
  }

  public static List<Rule> compose(final JvmTarget target, RuleType ruleType) {
    List<String> deps =
        ImmutableList.<String>builder()
            .addAll(external(getExternalDeps(target.getMain(), target.getProvided())))
            .addAll(targets(getTargetDeps(target.getMain(), target.getProvided())))
            .build();

    Set<String> aptDeps =
        ImmutableSet.<String>builder()
            .addAll(externalApt(target.getApt().getExternalJarDeps()))
            .addAll(targetsApt(target.getApt().getTargetDeps()))
            .build();

    Set<String> providedDeps =
        ImmutableSet.<String>builder()
            .addAll(external(getExternalProvidedDeps(target.getMain(), target.getProvided())))
            .addAll(targets(getTargetProvidedDeps(target.getMain(), target.getProvided())))
            .build();

    List<String> testTargets =
        !target.getTest().getSources().isEmpty()
            ? ImmutableList.of(":" + test(target))
            : ImmutableList.of();

    ImmutableList.Builder<Rule> rulesBuilder = new ImmutableList.Builder<>();
    rulesBuilder.add(
        new JvmRule()
            .srcs(target.getMain().getSources())
            .exts(ruleType.getSourceExtensions())
            .apPlugins(getApPlugins(target.getApPlugins()))
            .aptDeps(aptDeps)
            .providedDeps(providedDeps)
            .resources(target.getMain().getJavaResources())
            .sourceCompatibility(target.getSourceCompatibility())
            .targetCompatibility(target.getTargetCompatibility())
            .mavenCoords(target.getMavenCoords())
            .testTargets(testTargets)
            .options(mapOptions(target.getMain().getCompilerOptions()))
            .libDeps(target.hasApplication() ? deps : ImmutableSet.of())
            .ruleType(ruleType.getBuckName())
            .defaultVisibility()
            .deps(target.hasApplication() ? ImmutableSet.of() : deps)
            .name(src(target))
            .extraBuckOpts(target.getExtraOpts(ruleType)));

    if (target.hasApplication()) {
      rulesBuilder.add(
          new JvmBinaryRule()
              .mainClassName(target.getMainClass())
              .excludes(target.getExcludes())
              .defaultVisibility()
              .name(bin(target))
              .ruleType(RuleType.JAVA_BINARY.getBuckName())
              .extraBuckOpts(target.getExtraOpts(RuleType.JAVA_BINARY)));
    }

    return rulesBuilder.build();
  }
}
