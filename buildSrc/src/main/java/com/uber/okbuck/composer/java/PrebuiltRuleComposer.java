package com.uber.okbuck.composer.java;

import static com.uber.okbuck.core.dependency.ExternalDependency.AAR;
import static com.uber.okbuck.core.dependency.ExternalDependency.JAR;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.uber.okbuck.composer.jvm.JvmBuckRuleComposer;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.template.core.Rule;
import com.uber.okbuck.template.java.Prebuilt;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PrebuiltRuleComposer extends JvmBuckRuleComposer {

  /**
   * @param dependencies External Dependencies whose rule needs to be created
   * @return List of rules
   */
  @SuppressWarnings("NullAway")
  public static List<Rule> compose(Collection<ExternalDependency> dependencies) {
    return dependencies
        .stream()
        .filter(dependency -> ImmutableList.of(JAR, AAR).contains(dependency.getPackaging()))
        .sorted(
            (o1, o2) ->
                ComparisonChain.start()
                    .compare(o1.getPackaging(), o2.getPackaging())
                    .compare(o1.getCacheName(), o2.getCacheName())
                    .result())
        .map(
            dependency -> {
              String source = dependency.hasSourceFile() ? dependency.getSourceFileName() : null;

              RuleType ruleType;

              switch (dependency.getPackaging()) {
                case JAR:
                  ruleType = RuleType.PREBUILT_JAR;
                  break;
                case AAR:
                  ruleType = RuleType.ANDROID_PREBUILT_AAR;
                  break;
                default:
                  throw new IllegalStateException("Dependency not a valid prebuilt: " + dependency);
              }

              ImmutableList.Builder<Rule> rulesBuilder = ImmutableList.builder();
              rulesBuilder.add(
                  new Prebuilt()
                      .prebuiltType(ruleType.getProperties().get(0))
                      .prebuilt(dependency.getDependencyFileName())
                      .mavenCoords(dependency.getMavenCoords())
                      .enableJetifier(dependency.enableJetifier())
                      .source(source)
                      .ruleType(ruleType.getBuckName())
                      .name(dependency.getCacheName()));

              return rulesBuilder.build();
            })
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }
}
