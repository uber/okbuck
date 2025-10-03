package com.uber.okbuck.composer.java;

import static com.uber.okbuck.core.dependency.OResolvedDependency.AAR;
import static com.uber.okbuck.core.dependency.OResolvedDependency.JAR;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.composer.jvm.JvmBuckRuleComposer;
import com.uber.okbuck.core.dependency.OExternalDependency;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.template.core.Rule;
import com.uber.okbuck.template.java.Prebuilt;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class PrebuiltRuleComposer extends JvmBuckRuleComposer {

  private PrebuiltRuleComposer() {}

  // Visibile for testing
  static ImmutableSet<String> getLabels(OExternalDependency dependency, @Nullable Map<String, List<String>> labelsMap) {
    List<String> labels = (labelsMap == null)
      ? Collections.emptyList()
      : labelsMap.getOrDefault(dependency.getMavenCoordsForValidation(), Collections.emptyList());

    return labels == null ? ImmutableSet.of() : ImmutableSet.copyOf(labels);
  }

  /**
   * @param dependencies External Dependencies whose rule needs to be created
   * @return List of rules
   */
  @SuppressWarnings("NullAway")
  public static List<Rule> compose(
      Collection<OExternalDependency> dependencies, HashMap<String, String> shaSum256) {
    return compose(dependencies, shaSum256, null);
  }

  public static List<Rule> compose(
      Collection<OExternalDependency> dependencies, HashMap<String, String> shaSum256, Map<String, List<String>> labelsMap) {
    return dependencies
        .stream()
        .peek(
            dependency -> {
              if (!ImmutableSet.of(JAR, AAR).contains(dependency.getPackaging())) {
                throw new IllegalStateException("Dependency not a valid prebuilt: " + dependency);
              }
            })
        .sorted(OExternalDependency.compareByName)
        .map(
            dependency -> {
              String sha256Key =
                  OExternalDependency.getGradleSha(dependency.getRealDependencyFile());
              String sha256 = Preconditions.checkNotNull(shaSum256.get(sha256Key));

              Prebuilt rule =
                  new Prebuilt()
                      .mavenCoords(dependency.getMavenCoords())
                      .enableJetifier(dependency.enableJetifier())
                      .firstLevel(dependency.isFirstLevel())
                      .sha256(sha256);

              dependency
                  .getRealSourceFile()
                  .ifPresent(
                      file -> {
                        String sourcesSha256Key = OExternalDependency.getGradleSha(file);
                        String sourcesSha256 =
                            Preconditions.checkNotNull(shaSum256.get(sourcesSha256Key));
                        rule.sourcesSha256(sourcesSha256);
                      });

              rule.labels(getLabels(dependency, labelsMap));

              rule.ruleType(RuleType.PREBUILT.getBuckName())
                  .deps(external(dependency.getDeps()))
                  .name(dependency.getTargetName());

              return rule;
            })
        .collect(Collectors.toList());
  }
}
