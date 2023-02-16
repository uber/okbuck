package com.uber.okbuck.core.dependency.exporter;

import org.codehaus.plexus.util.StringUtils;
import org.gradle.api.artifacts.ExternalDependency;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class DependencyExporterModel {

  @Nullable
  private final String name;
  @Nullable
  private final String version;
  @Nullable
  private final String group;
  private final boolean force;
  @Nullable
  private Set<String> excludeRules = new TreeSet<>();
  private final boolean transitive;

  public DependencyExporterModel(ExternalDependency externalDependency) {
    name = externalDependency.getName();
    version = externalDependency.getVersion();
    group = externalDependency.getGroup();
    force = externalDependency.isForce();
    transitive = externalDependency.isTransitive();

    if (externalDependency.getExcludeRules() != null) {
      excludeRules = externalDependency.getExcludeRules().stream().flatMap(e -> {
        Set<String> set = new TreeSet<>();
        if (StringUtils.isNotBlank(e.getGroup()) && StringUtils.isNotBlank(e.getModule())) {
          set.add(String.format("%s:%s", e.getGroup(), e.getModule()));
        } else if (StringUtils.isNotBlank(e.getGroup())) {
          set.add(e.getGroup());
        } else if (StringUtils.isNotBlank(e.getModule())) {
          set.add(e.getModule());
        }
        return set.stream();
      }).collect(Collectors.toSet());
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
