package com.uber.okbuck.composer.java;

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

  /**
   * @param dependencies External Dependencies whose rule needs to be created
   * @return List of rules
   */
  public static List<Rule> compose(Collection<ExternalDependency> dependencies) {
    return dependencies
        .stream()
        .filter(
            dependency -> dependency.packaging.equals("aar") || dependency.packaging.equals("jar"))
        .sorted(
            (o1, o2) ->
                ComparisonChain.start()
                    .compare(o1.packaging, o2.packaging)
                    .compare(o1.getCacheName(), o2.getCacheName())
                    .result())
        .map(
            dependency -> {
              String source = null;
              if (dependency.hasSourceJar()) {
                source = dependency.getSourceFileName();
              }

              String ruleType;
              String prebuiltType;
              String mavenCoords;

              switch (dependency.packaging) {
                case "jar":
                  ruleType = "prebuilt_jar";
                  prebuiltType = "binary_jar";
                  mavenCoords = dependency.getMavenCoords();
                  break;
                case "aar":
                  ruleType = "android_prebuilt_aar";
                  prebuiltType = "aar";
                  mavenCoords = null;
                  break;
                default:
                  throw new IllegalStateException("Dependency not a valid prebuilt: " + dependency);
              }

              ImmutableList.Builder<Rule> rulesBuilder = ImmutableList.builder();
              rulesBuilder.add(
                  new Prebuilt()
                      .prebuiltType(prebuiltType)
                      .prebuilt(dependency.getDepFileName())
                      .mavenCoords(mavenCoords)
                      .source(source)
                      .ruleType(ruleType)
                      .name(dependency.getCacheName()));

              if (dependency.hasLintJar()) {
                rulesBuilder.add(
                    new Prebuilt()
                        .prebuiltType("binary_jar")
                        .prebuilt(dependency.getLintFileName())
                        .ruleType("prebuilt_jar")
                        .name(dependency.getLintCacheName()));
              }

              return rulesBuilder.build();
            })
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }
}
