package com.uber.okbuck.composer.common;

import com.google.common.base.Preconditions;
import com.uber.okbuck.core.dependency.OExternalDependency;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.template.common.BazelHttpFile;
import com.uber.okbuck.template.core.Rule;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class BazelHttpFileRuleComposer {

  private BazelHttpFileRuleComposer() {}

  /**
   * @param dependencies External Dependencies whose http file rule needs to be created
   * @return List of rules
   */
  public static List<Rule> compose(
      Collection<OExternalDependency> dependencies, HashMap<String, String> shaSum256) {
    return dependencies
        .stream()
        .sorted(OExternalDependency.compareByName)
        .map(
            dependency -> {
              String sha256Key =
                  OExternalDependency.getGradleSha(dependency.getRealDependencyFile());
              String sha256 = Preconditions.checkNotNull(shaSum256.get(sha256Key));

              BazelHttpFile rule =
                  new BazelHttpFile().mavenCoords(dependency.getMavenCoords()).sha256(sha256);

              dependency
                  .getRealSourceFile()
                  .ifPresent(
                      file -> {
                        String sourcesSha256Key = OExternalDependency.getGradleSha(file);
                        String sourcesSha256 =
                            Preconditions.checkNotNull(shaSum256.get(sourcesSha256Key));
                        rule.sourcesSha256(sourcesSha256);
                      });

              rule.ruleType(RuleType.HTTP_FILE.getBuckName());
              return rule;
            })
        .collect(Collectors.toList());
  }
}
