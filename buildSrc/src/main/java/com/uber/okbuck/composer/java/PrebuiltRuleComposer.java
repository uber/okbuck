package com.uber.okbuck.composer.java;

import static com.uber.okbuck.core.dependency.ExternalDependency.AAR;
import static com.uber.okbuck.core.dependency.ExternalDependency.JAR;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.uber.okbuck.composer.jvm.JvmBuckRuleComposer;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.template.core.Rule;
import com.uber.okbuck.template.java.Prebuilt;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PrebuiltRuleComposer extends JvmBuckRuleComposer {

  public static final String JAVA_PREBUILT_JAR = "prebuilt_jar";
  public static final String BINARY_JAR = "binary_jar";
  private static final String ANDROID_PREBUILT_AAR = "android_prebuilt_aar";

  /**
   * @param dependencies External Dependencies whose rule needs to be created
   * @return List of rules
   */
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
              String source = null;
              if (dependency.hasSourceFile()) {
                source = dependency.getSourceFileName();
              }

              String ruleType;
              String prebuiltType;

              switch (dependency.getPackaging()) {
                case JAR:
                  ruleType = JAVA_PREBUILT_JAR;
                  prebuiltType = BINARY_JAR;
                  break;
                case AAR:
                  ruleType = ANDROID_PREBUILT_AAR;
                  prebuiltType = AAR;
                  break;
                default:
                  throw new IllegalStateException("Dependency not a valid prebuilt: " + dependency);
              }

              ImmutableList.Builder<Rule> rulesBuilder = ImmutableList.builder();
              rulesBuilder.add(
                  new Prebuilt()
                      .prebuiltType(prebuiltType)
                      .prebuilt(dependency.getDependencyFileName())
                      .mavenCoords(dependency.getMavenCoords())
                      .source(source)
                      .ruleType(ruleType)
                      .name(dependency.getCacheName()));

              if (dependency.hasLintFile()) {
                rulesBuilder.add(
                    new Prebuilt()
                        .prebuiltType(BINARY_JAR)
                        .prebuilt(dependency.getLintFileName())
                        .ruleType(JAVA_PREBUILT_JAR)
                        .name(dependency.getLintCacheName()));
              }

              return rulesBuilder.build();
            })
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }
}
