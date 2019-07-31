package com.uber.okbuck.composer.common;

import com.google.common.base.Preconditions;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.template.common.HttpFile;
import com.uber.okbuck.template.core.Rule;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class HttpFileRuleComposer {

  private HttpFileRuleComposer() {}

  /**
   * @param dependencies External Dependencies whose http file rule needs to be created
   * @return List of rules
   */
  public static List<Rule> compose(
      Collection<ExternalDependency> dependencies, HashMap<String, String> shaSum256) {
    return dependencies
        .stream()
        .sorted(ExternalDependency.compareByName)
        .map(
            dependency -> {
              String sha256Key =
                  ExternalDependency.getGradleSha(dependency.getRealDependencyFile());
              String sha256 = Preconditions.checkNotNull(shaSum256.get(sha256Key));

              Rule rule =
                  new HttpFile()
                      .mavenCoords(dependency.getMavenCoords())
                      .sha256(sha256)
                      .name(dependency.getTargetName());
              rule.name(dependency.getTargetName());
              return rule;
            })
        .collect(Collectors.toList());
  }
}
