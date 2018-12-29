package com.uber.okbuck.composer.common;

import com.uber.okbuck.core.dependency.DependencyUtils;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.template.common.HttpFile;
import com.uber.okbuck.template.core.Rule;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class HttpFileRuleComposer {

  private HttpFileRuleComposer() {}

  /**
   * @param dependencies External Dependencies whose http file rule needs to be created
   * @return List of rules
   */
  public static List<Rule> compose(Collection<ExternalDependency> dependencies) {
    return dependencies
        .stream()
        .sorted(ExternalDependency.compareByName)
        .map(
            dependency -> {
              Rule rule =
                  new HttpFile()
                      .mavenCoords(dependency.getMavenCoords())
                      .sha256(DependencyUtils.shaSum256(dependency.getRealDependencyFile()))
                      .name(dependency.getTargetName());
              rule.name(dependency.getTargetName());
              return rule;
            })
        .collect(Collectors.toList());
  }
}
