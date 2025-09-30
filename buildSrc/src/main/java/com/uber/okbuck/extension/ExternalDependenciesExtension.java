package com.uber.okbuck.extension;

import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.VersionlessDependency;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.gradle.api.tasks.Input;

public class ExternalDependenciesExtension {
  /** Specifies the folder where all external dependency rules gets generated. */
  @Input private String cache = ".okbuck/ext";

  /**
   * Specifies to delete the cache dir, before generating dependency rules If set to false, only
   * deletes the existing dependency rules files.
   */
  @Input private boolean cleanCacheDir = true;

  /** Specifies whether the external dependencies should be downloaded by buck or not. */
  @Input private boolean downloadInBuck = true;

  /**
   * Specifies whether the maven_repositories block should be written to the okbuck.buckconfig file
   * or not.
   */
  @Input private boolean generateMavenRepositories = true;

  /** Specifies what resolution action to use for external dependencies. */
  @Input private ResolutionAction resolutionAction = ResolutionAction.ALL;

  /** Specifies whether to enable exported_deps for external dependencies or not. */
  @Input private boolean enableExportedDeps = false;

  /** Specifies whether to enable exported_deps for external dependencies or not. */
  @Input private boolean thirdPartyResolutionOnly = false;

  /** Specifies whether to mark deps of first level external dependencies visible or not. */
  @Input private boolean strictVisibility = false;

  @Input private boolean markFirstLevelAllVersions = true;

  @Input private Set<String> autoValueConfigurations = new HashSet<>();

  /**
   * Stores the dependencies which are allowed to have more than 1 version. This is needed for few
   * dependencies like robolectric runtime deps.
   */
  @Input
  private List<String> allowAllVersions =
      Collections.singletonList("org.robolectric:android-all-instrumented");

  /**
   * Stores the dependency versions to be used for dynamic notations that have , or + in their
   * versions
   */
  @Input private Map<String, String> dynamicDependencyVersionMap = new HashMap<>();

  /**
   * Stores the dynamic dependencies to ignore if they are resolved with other gradle resolution
   * mechanisms
   */
  @Input private Set<String> dynamicDependenciesToIgnore = new HashSet<>();

  /** Set to true to enable creation of http_file rules needed by bazel build system */
  @Input private boolean bazelDeps = false;

  /** Set the path to the sha256sum caches of external dependency artifacts */
  @Input private String sha256Cache = OkBuckGradlePlugin.DEFAULT_OKBUCK_SHA256;

  /** Map of dependency coordinates to labels for prebuilt rules */
  @Input private Map<String, List<String>> labelsMap = new HashMap<>();

  @Nullable private Set<VersionlessDependency> allowAllVersionsSet;

  public ExternalDependenciesExtension() {}

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

  public boolean useLatest() {
    return resolutionAction.equals(ResolutionAction.LATEST);
  }

  public boolean useLatest(VersionlessDependency versionlessDependency) {
    if (getAllowAllVersionsSet().contains(versionlessDependency)) {
      return false;
    }

    return useLatest();
  }

  public boolean useSingle() {
    return resolutionAction.equals(ResolutionAction.SINGLE);
  }

  public boolean useSingle(VersionlessDependency versionlessDependency) {
    if (getAllowAllVersionsSet().contains(versionlessDependency)) {
      return false;
    }

    return useSingle();
  }

  public boolean strictVisibilityEnabled() {
    return exportedDepsEnabled() && this.strictVisibility;
  }

  public boolean shouldMarkFirstLevelAllVersions() {
    return markFirstLevelAllVersions;
  }

  public boolean resoleOnlyThirdParty() {
    return versionedExportedDepsEnabled() && this.thirdPartyResolutionOnly;
  }

  public boolean versionlessEnabled() {
    return useLatest() || useSingle();
  }

  public boolean exportedDepsEnabled() {
    return enableExportedDeps;
  }

  public boolean versionlessExportedDepsEnabled() {
    return versionlessEnabled() && enableExportedDeps;
  }

  public boolean versionedExportedDepsEnabled() {
    return !versionlessEnabled() && enableExportedDeps;
  }

  public boolean isVersioned(VersionlessDependency versionlessDependency) {
    if (getAllowAllVersionsSet().contains(versionlessDependency)) {
      return true;
    }

    return !versionlessEnabled();
  }

  public String getCache() {
    return cache;
  }

  public boolean shouldDownloadInBuck() {
    return downloadInBuck;
  }

  public boolean getGenerateMavenRepositories() {
    return generateMavenRepositories;
  }

  public Set<String> getAutoValueConfigurations() {
    return autoValueConfigurations;
  }

  public Map<String, String> getDynamicDependencyVersionMap() {
    return dynamicDependencyVersionMap;
  }

  public Set<String> getDynamicDependenciesToIgnore() {
    return dynamicDependenciesToIgnore;
  }

  public boolean bazelDepsEnabled() {
    return bazelDeps;
  }

  public String getSha256Cache() {
    return sha256Cache;
  }

  public boolean shouldCleanCacheDir() {
    return cleanCacheDir;
  }

  public Map<String, List<String>> getLabelsMap() {
    return labelsMap;
  }
}
