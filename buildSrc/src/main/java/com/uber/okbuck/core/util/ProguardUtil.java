package com.uber.okbuck.core.util;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.VersionlessDependency;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public final class ProguardUtil {

    private static final String PROGUARD_GROUP = "net.sf.proguard";
    private static final String PROGUARD_MODULE = "proguard-base";
    private static final String PROGUARD_DEPS_CACHE = OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/proguard";

    private ProguardUtil() {}

    @Nullable
    public static String getProguardJarPath(Project project) {
        Configuration proguardConfiguration = project.getConfigurations().detachedConfiguration(
                new DefaultExternalModuleDependency(PROGUARD_GROUP, PROGUARD_MODULE,
                        getProguardVersion(project)));
        DependencyCache proguardCache = new DependencyCache("proguard",
                project.getRootProject(),
                PROGUARD_DEPS_CACHE,
                Collections.singleton(proguardConfiguration));
        String proguardJarPath = null;
        try {
            proguardJarPath = proguardCache.get(new VersionlessDependency(PROGUARD_GROUP, PROGUARD_MODULE));
        } catch (IllegalStateException ignored) {}
        return proguardJarPath;
    }

    @Nullable
    private static String getProguardVersion(Project project) {
        return project.getBuildscript()
                .getConfigurations()
                .getByName("classpath")
                .getResolvedConfiguration()
                .getResolvedArtifacts()
                .stream()
                .filter(ProguardUtil::findProguard)
                .findFirst()
                .map(r -> r.getModuleVersion().getId().getVersion())
                .orElse(null);
    }

    private static boolean findProguard(ResolvedArtifact artifact) {
        ModuleVersionIdentifier identifier = artifact.getModuleVersion().getId();
        return (PROGUARD_GROUP.equals(identifier.getGroup()) && PROGUARD_MODULE.equals(identifier.getName()));
    }
}
