package com.uber.okbuck.core.dependency;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.artifacts.ExcludeRule;

@AutoValue
public abstract class OUnresolvedDependency {
  public abstract VersionlessDependency versionless();

  public abstract String version();

  public abstract ImmutableSet<ExcludeRule> excludeRules();

  public static Builder builder() {
    return new AutoValue_OUnresolvedDependency.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setVersionless(VersionlessDependency value);

    public abstract Builder setVersion(String value);

    public abstract Builder setExcludeRules(Set<ExcludeRule> excludeRules);

    public abstract OUnresolvedDependency build();
  }

  public List<Map<String, String>> excludeProperties() {
    return excludeRules()
        .stream()
        .map(
            rule -> {
              Map<String, String> props = new HashMap<>();

              props.put(ExcludeRule.GROUP_KEY, rule.getGroup());
              props.put(ExcludeRule.MODULE_KEY, rule.getModule());

              return props;
            })
        .collect(Collectors.toList());
  }
}
