package com.uber.okbuck.composer.java;

import static com.uber.okbuck.core.dependency.BaseExternalDependency.AAR;
import static com.uber.okbuck.core.dependency.BaseExternalDependency.JAR;

import com.uber.okbuck.composer.jvm.JvmBuckRuleComposer;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.template.core.Rule;
import com.uber.okbuck.template.java.NativePrebuilt;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class LocalPrebuiltRuleComposer extends JvmBuckRuleComposer {
  /**
   * @param dependencies Local Dependencies whose rule needs to be created
   * @return List of rules
   */
  @SuppressWarnings("NullAway")
  public static List<Rule> compose(Collection<ExternalDependency> dependencies) {
    return dependencies
        .stream()
        .sorted(ExternalDependency.compareByName)
        .map(
            dependency -> {
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

              String source;
              if (dependency.getRealSourceFile().isPresent()) {
                source = dependency.getSourceFileName();
              } else {
                source = null;
              }

              return new NativePrebuilt()
                  .prebuiltType(ruleType.getProperties().get(0))
                  .prebuilt(dependency.getDependencyFileName())
                  .mavenCoords(dependency.getMavenCoords())
                  .enableJetifier(dependency.enableJetifier())
                  .source(source)
                  .ruleType(ruleType.getBuckName())
                  .deps(external(dependency.getDeps()))
                  .name(dependency.getTargetName());
            })
        .collect(Collectors.toList());
  }
}
