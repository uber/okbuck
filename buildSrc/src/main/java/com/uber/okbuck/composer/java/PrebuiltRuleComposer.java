package com.uber.okbuck.composer.java;

import static com.uber.okbuck.core.dependency.BaseExternalDependency.AAR;
import static com.uber.okbuck.core.dependency.BaseExternalDependency.JAR;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.composer.jvm.JvmBuckRuleComposer;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.template.core.Rule;
import com.uber.okbuck.template.java.Prebuilt;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class PrebuiltRuleComposer extends JvmBuckRuleComposer {

  private PrebuiltRuleComposer() {}

  /**
   * @param dependencies External Dependencies whose rule needs to be created
   * @return List of rules
   */
  @SuppressWarnings("NullAway")
  public static List<Rule> compose(
      Collection<ExternalDependency> dependencies, HashMap<String, String> shaSum256) {
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
              String sha256Key =
                  ExternalDependency.getGradleSha(dependency.getRealDependencyFile());
              String sha256 = Preconditions.checkNotNull(shaSum256.get(sha256Key));

              Prebuilt rule =
                  new Prebuilt()
                      .mavenCoords(dependency.getMavenCoords())
                      .enableJetifier(dependency.enableJetifier())
                      .sha256(sha256);

              dependency
                  .getRealSourceFile()
                  .ifPresent(
                      file -> {
                        String sourcesSha256Key = ExternalDependency.getGradleSha(file);
                        String sourcesSha256 =
                            Preconditions.checkNotNull(shaSum256.get(sourcesSha256Key));
                        rule.sourcesSha256(sourcesSha256);
                      });

              rule.ruleType(RuleType.PREBUILT.getBuckName())
                  .deps(external(dependency.getDeps()))
                  .name(dependency.getTargetName());

              return rule;
            })
        .collect(Collectors.toList());
  }
}
