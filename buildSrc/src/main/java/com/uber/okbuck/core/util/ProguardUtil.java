package com.uber.okbuck.core.util;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.DependencyUtils;
import com.uber.okbuck.core.dependency.ExternalDependency;
import java.util.Optional;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;
import org.jetbrains.annotations.Nullable;

public final class ProguardUtil {

  private static final String PROGUARD_GROUP = "net.sf.proguard";
  private static final String PROGUARD_MODULE = "proguard-base";
  private static final String PROGUARD_DEPS_CACHE =
      OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/proguard";

  private ProguardUtil() {}

  @Nullable
  public static String getProguardJarPath(Project project) {
    String proguardVersion =
        ProjectUtil.findVersionInClasspath(project, PROGUARD_GROUP, PROGUARD_MODULE);
    Configuration proguardConfiguration =
        project
            .getConfigurations()
            .detachedConfiguration(
                new DefaultExternalModuleDependency(
                    PROGUARD_GROUP, PROGUARD_MODULE, proguardVersion));
    DependencyCache proguardCache =
        new DependencyCache(project, DependencyUtils.createCacheDir(project, PROGUARD_DEPS_CACHE));
    proguardCache.build(proguardConfiguration);
    String proguardJarPath = null;
    try {
      Optional<ResolvedArtifactResult> artifactResult =
          proguardConfiguration
              .getIncoming()
              .getArtifacts()
              .getArtifacts()
              .stream()
              .filter(
                  artifact -> {
                    ComponentIdentifier identifier = artifact.getId().getComponentIdentifier();
                    return identifier instanceof ModuleComponentIdentifier
                        && ((ModuleComponentIdentifier) identifier)
                            .getGroup()
                            .equals(PROGUARD_GROUP)
                        && ((ModuleComponentIdentifier) identifier)
                            .getModule()
                            .equals(PROGUARD_MODULE);
                  })
              .findFirst();

      if (artifactResult.isPresent()) {
        proguardJarPath =
            proguardCache.get(
                new ExternalDependency(
                    PROGUARD_GROUP,
                    PROGUARD_MODULE,
                    proguardVersion,
                    artifactResult.get().getFile()),
                true,
                false);
      }
    } catch (IllegalStateException ignored) {
    }
    return proguardJarPath;
  }
}
