package com.uber.okbuck.core.util;

import com.google.errorprone.annotations.Var;
import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.OExternalDependency;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;

public final class ProguardUtil {

  private static final String PROGUARD_GROUP = "net.sf.proguard";
  private static final String PROGUARD_NEW_GROUP = "com.guardsquare";
  private static final String PROGUARD_MODULE = "proguard-base";

  private ProguardUtil() {}

  @Nullable
  public static String getProguardJarPath(Project project) {
    @Var
    String proguardVersion =
        ProjectUtil.findVersionInClasspath(project, PROGUARD_NEW_GROUP, PROGUARD_MODULE);
    @Var String currentGroup = PROGUARD_NEW_GROUP;

    if (proguardVersion == null) {
      proguardVersion =
          ProjectUtil.findVersionInClasspath(project, PROGUARD_GROUP, PROGUARD_MODULE);
      currentGroup = PROGUARD_GROUP;
    }

    Configuration proguardConfiguration =
        project
            .getConfigurations()
            .detachedConfiguration(
                new DefaultExternalModuleDependency(
                    currentGroup, PROGUARD_MODULE, proguardVersion));

    DependencyCache cache = new DependencyCache(project, ProjectUtil.getDependencyManager(project));
    Set<OExternalDependency> dependencies = cache.build(proguardConfiguration);

    Optional<OExternalDependency> proguardDependency =
        dependencies
            .stream()
            .filter(
                dependency ->
                    (dependency.getGroup().equals(PROGUARD_GROUP)
                            || dependency.getGroup().equals(PROGUARD_NEW_GROUP))
                        && dependency.getName().equals(PROGUARD_MODULE))
            .findAny();

    return proguardDependency.map(BuckRuleComposer::external).orElse(null);
  }
}
