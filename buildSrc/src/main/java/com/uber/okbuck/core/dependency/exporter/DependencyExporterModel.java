package com.uber.okbuck.core.dependency.exporter;

import org.codehaus.plexus.util.StringUtils;
import org.gradle.api.artifacts.ExternalDependency;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class DependencyExporterModel {

  @Nullable private final String name;
  @Nullable private final String version;
  @Nullable private final String group;
  private final boolean force;
  @Nullable private Set<String> excludeRules = new TreeSet<>();

  public DependencyExporterModel(ExternalDependency externalDependency) {
    name = externalDependency.getName();
    version = externalDependency.getVersion();
    group = externalDependency.getGroup();
    force = externalDependency.isForce();

    if (externalDependency.getExcludeRules() != null) {
      excludeRules =
          externalDependency.getExcludeRules()
              .stream()
              .map(
                  e -> {
                    if (StringUtils.isNotBlank(e.getGroup())
                        && StringUtils.isNotBlank(e.getModule())) {
                      return String.format("%s:%s", e.getGroup(), e.getModule());
                    }

                    if (StringUtils.isNotBlank(e.getGroup())) {
                      return e.getGroup();
                    }

                    if (StringUtils.isNotBlank(e.getModule())) {
                      return e.getModule();
                    }
                    return null;
                  })
              .collect(Collectors.toSet());
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
}
