package com.uber.okbuck.core.dependency.exporter;

import org.gradle.api.artifacts.ExcludeRule;
import org.gradle.api.artifacts.ExternalDependency;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.TreeSet;

public class DependencyExporterModel {

  @Nullable
  private final String name;
  @Nullable
  private final String version;
  @Nullable
  private final String group;
  private final boolean force;
  @Nullable
  private final Set<String> excludeRules;
  private final boolean transitive;

  public DependencyExporterModel(ExternalDependency externalDependency) {
    name = externalDependency.getName();
    version = externalDependency.getVersion();
    group = externalDependency.getGroup();
    force = externalDependency.isForce();
    transitive = externalDependency.isTransitive();

    excludeRules = new TreeSet<>();

    if (externalDependency.getExcludeRules() != null) {
      for (ExcludeRule excludeRule : externalDependency.getExcludeRules()) {
        if (excludeRule.getGroup() != null) {
          excludeRules.add(excludeRule.getGroup());
        }
        if (excludeRule.getModule() != null) {
          excludeRules.add(excludeRule.getModule());
        }
      }
    }
  }

  @Nullable
  public String getName() {
    return name;
  }

  @Nullable
  public String getVersion() {
    return version;
  }

  @Nullable
  public String getGroup() {
    return group;
  }

  public boolean isForce() {
    return force;
  }

  @Nullable
  public Set<String> getExcludeRules() {
    return excludeRules;
  }

  public boolean isTransitive() {
    return transitive;
  }
}
