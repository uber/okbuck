package com.uber.okbuck.composer.java;

import static com.uber.okbuck.core.dependency.BaseExternalDependency.AAR;
import static com.uber.okbuck.core.dependency.BaseExternalDependency.JAR;

import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.composer.jvm.JvmBuckRuleComposer;
import com.uber.okbuck.core.dependency.DependencyUtils;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.template.core.Rule;
import com.uber.okbuck.template.java.Prebuilt;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PrebuiltRuleComposer extends JvmBuckRuleComposer {

  private PrebuiltRuleComposer() {}

  /**
   * @param dependencies External Dependencies whose rule needs to be created
   * @return List of rules
   */
  @SuppressWarnings("NullAway")
  public static List<Rule> compose(Collection<ExternalDependency> dependencies) {
    return dependencies
        .stream()
        .peek(
            dependency -> {
              if (!ImmutableSet.of(JAR, AAR).contains(dependency.getPackaging())) {
                throw new IllegalStateException("Dependency not a valid prebuilt: " + dependency);
              }
            })
        .sorted(ExternalDependency.compareByName)
        .map(
            dependency -> {
              Prebuilt rule =
                  new Prebuilt()
                      .mavenCoords(dependency.getMavenCoords())
                      .enableJetifier(dependency.enableJetifier())
                      .sha256(DependencyUtils.shaSum256(dependency.getRealDependencyFile()));

              if (dependency.hasSourceFile()) {
                rule.sourcesSha256(DependencyUtils.shaSum256(dependency.getRealSourceFile()));
              }

              rule.name(dependency.getTargetName());
              rule.ruleType(RuleType.PREBUILT.getBuckName());
              return rule;
            })
        .collect(Collectors.toList());
  }
}
