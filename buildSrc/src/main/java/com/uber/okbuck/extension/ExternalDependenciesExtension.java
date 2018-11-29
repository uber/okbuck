package com.uber.okbuck.extension;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.dependency.VersionlessDependency;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;

public class ExternalDependenciesExtension {
  /** Specifies the folder where all external dependency rules gets generated. */
  @Input private String cache = ".okbuck/ext";

  /** Stores the dependencies which are allowed to have latest version. */
  @Input private List<String> allowLatestVersion = new ArrayList<>();

  /** Stores the dependencies which are allowed to have more than 1 version. */
  @Input private List<String> allowAllVersions = new ArrayList<>();

  /** Stores the dependencies and their allowed versions */
  @Input private Map<String, List<String>> allowSpecificVersions = new HashMap<>();

  private boolean versionless = false;
  private boolean allowLatestForAll = false;

  @Nullable private Set<VersionlessDependency> allowLatestVersionSet;
  @Nullable private Set<VersionlessDependency> allowAllVersionsSet;
  @Nullable private Map<VersionlessDependency, List<String>> allowedVersionsMap;

  public ExternalDependenciesExtension(Project project) {
    project.afterEvaluate(
        evaluatedProject -> {
          validateExtension();
        });
  }

  private void validateExtension() {
    Set<VersionlessDependency> allowLatest = getAllowLatestVersionSet();
    Set<VersionlessDependency> allowAll = getAllowAllVersionsSet();
    Set<VersionlessDependency> allowSpecific = getAllowSpecificVersionsMap().keySet();

    checkIntersection(
        Sets.intersection(allowLatest, allowAll), "allowLatestVersion", "allowAllVersions");
    checkIntersection(
        Sets.intersection(allowLatest, allowSpecific),
        "allowLatestVersion",
        "allowSpecificVersions");
    checkIntersection(
        Sets.intersection(allowAll, allowSpecific), "allowAllVersions", "allowSpecificVersions");
  }

  private static void checkIntersection(
      Sets.SetView<VersionlessDependency> intersect, String setA, String setB) {
    String intersectErrorString =
        intersect
            .stream()
            .map(VersionlessDependency::mavenCoords)
            .collect(Collectors.joining("\n"));

    Preconditions.checkArgument(
        intersect.size() == 0,
        String.format(
            "'%s' found in both '%s' & '%s', please remove from one of them.",
            intersectErrorString, setA, setB));
  }

  private synchronized Map<VersionlessDependency, List<String>> getAllowSpecificVersionsMap() {
    if (allowedVersionsMap == null) {
      allowedVersionsMap =
          allowSpecificVersions
              .entrySet()
              .stream()
              .peek(
                  entry ->
                      Preconditions.checkArgument(
                          entry.getValue().size() > 1,
                          String.format(
                              "%s should have more than one versions specified in 'allowSpecificVersions'",
                              entry.getKey())))
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
              .collect(ImmutableSet.toImmutableSet());
    }
    return allowAllVersionsSet;
  }

  private synchronized Set<VersionlessDependency> getAllowLatestVersionSet() {
    if (allowLatestVersionSet == null) {
      allowLatestVersionSet =
          allowLatestVersion
              .stream()
              .map(VersionlessDependency::fromMavenCoords)
              .collect(ImmutableSet.toImmutableSet());
    }
    return allowLatestVersionSet;
  }

  /**
   * Returns whether versionless is enabled or not. This is a global flag to turn on/off
   * versionless.
   *
   * @return boolean stating above.
   */
  public boolean versionlessEnabled() {
    return versionless;
  }

  /**
   * Returns whether using latest version is enabled for any dependency.
   *
   * @return boolean stating above.
   */
  public boolean allowLatestEnabled() {
    return allowLatestForAll || getAllowLatestVersionSet().size() > 0;
  }

  /**
   * Checks if the latest version should be used for this dependency.
   *
   * @param versionlessDependency dependency to check.
   * @return boolean stating above.
   */
  public boolean isAllowLatestFor(VersionlessDependency versionlessDependency) {
    if (!versionless) {
      return false;
    }

    return allowLatestForAll || getAllowLatestVersionSet().contains(versionlessDependency);
  }

  /**
   * Checks whether the given dependency should be versioned or not.
   *
   * @param versionlessDependency dependency to check.
   * @return boolean stating above.
   */
  public boolean isVersioned(VersionlessDependency versionlessDependency) {
    if (!versionlessEnabled()) {
      return true;
    }

    if (isAllowLatestFor(versionlessDependency)) {
      return false;
    }

    return getAllowAllVersionsSet().contains(versionlessDependency)
        || getAllowSpecificVersionsMap().containsKey(versionlessDependency);
  }

  /**
   * Checks whether the given versioned dependency is allowed.
   *
   * @param dependency dependency to check.
   * @return boolean stating above.
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean isAllowedVersion(ExternalDependency dependency) {
    if (!versionlessEnabled()) {
      return true;
    }

    if (getAllowAllVersionsSet().contains(dependency.getVersionless())) {
      return true;
    }

    List<String> allowedVersions = getAllowSpecificVersionsMap().get(dependency.getVersionless());
    return allowedVersions != null && allowedVersions.contains(dependency.getVersion());
  }

  public String getCache() {
    return cache;
  }
}
