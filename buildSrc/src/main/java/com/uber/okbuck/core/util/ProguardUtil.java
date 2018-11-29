package com.uber.okbuck.core.util;

import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.ExternalDependency;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
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

    DependencyCache cache = new DependencyCache(project, ProjectUtil.getDependencyManager(project));
    Set<ExternalDependency> dependencies = cache.build(proguardConfiguration);

    Optional<ExternalDependency> proguardDependency =
        dependencies
            .stream()
            .filter(
                dependency ->
                    dependency.getGroup().equals(PROGUARD_GROUP)
                        && dependency.getName().equals(PROGUARD_MODULE))
            .findAny();

    return proguardDependency.map(BuckRuleComposer::external).orElse(null);
  }
}
