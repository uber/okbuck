package com.uber.okbuck.extension;

import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.dependency.VersionlessDependency;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class ExternalDependenciesExtension {

  /** Stores the dependencies and their allowed versions */
  private Map<String, List<String>> allowSpecificVersions = new HashMap<>();

  /** Stores the dependencies which are allowed to have more than 1 version. */
  private List<String> allowAllVersions = new ArrayList<>();

  private boolean enableVersionLess = false;

  @Nullable private Map<VersionlessDependency, List<String>> allowedVersionsMap;
  @Nullable private Set<VersionlessDependency> allowAllVersionsSet;

  private synchronized Map<VersionlessDependency, List<String>> getAllowedVersionsMap() {
    if (allowedVersionsMap == null) {
      allowedVersionsMap =
          allowSpecificVersions
              .entrySet()
              .stream()
              .collect(
                  Collectors.toMap(
                      entry -> VersionlessDependency.fromMavenCoords(entry.getKey()),
                      Map.Entry::getValue));
    }
    return allowedVersionsMap;
  }

  private synchronized Set<VersionlessDependency> getAllowAllVersionsSet() {
    if (allowAllVersionsSet == null) {
      allowAllVersionsSet =
          allowAllVersions
              .stream()
              .map(VersionlessDependency::fromMavenCoords)
              .collect(Collectors.toSet());
    }
    return allowAllVersionsSet;
  }

  public boolean isEnableVersionLess() {
    return enableVersionLess;
  }

  public boolean isVersioned(VersionlessDependency versionless) {
    return !enableVersionLess
        || getAllowAllVersionsSet().contains(versionless)
        || getAllowedVersionsMap().containsKey(versionless);
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean isAllowed(ExternalDependency dependency) {
    if (!enableVersionLess) {
      return true;
    }

    if (getAllowAllVersionsSet().contains(dependency.getVersionless())) {
      return true;
    }

    List<String> allowedVersions = getAllowedVersionsMap().get(dependency.getVersionless());
    return allowedVersions != null && allowedVersions.contains(dependency.getVersion());
  }
}
