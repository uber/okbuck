package com.uber.okbuck.core.util;

import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.ExternalDependency;
import java.nio.file.Paths;
import java.util.Optional;
import javax.annotation.Nullable;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;

public final class ProguardUtil {

  private static final String PROGUARD_GROUP = "net.sf.proguard";
  private static final String PROGUARD_MODULE = "proguard-base";

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
        new DependencyCache(project, ProjectUtil.getDependencyManager(project));
    proguardCache.build(proguardConfiguration);
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
        ExternalDependency dependency =
            ExternalDependency.from(
                PROGUARD_GROUP,
                PROGUARD_MODULE,
                proguardVersion,
                artifactResult.get().getFile(),
                ProjectUtil.getOkBuckExtension(project).getExternalDependenciesExtension());

        return Paths.get(proguardCache.getPath(proguardCache.get(dependency, true)))
            .getParent()
            .resolve(dependency.getDependencyFileName())
            .toString();
      }
    } catch (IllegalStateException ignored) {
    }
    return null;
  }
}
