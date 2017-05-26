package com.uber.okbuck.core.util;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public final class KotlinUtil {

    private static final String KOTLIN_DEPS_CONFIG = "okbuck_kotlin_deps";
    private static final String KOTLIN_GROUP = "org.jetbrains.kotlin";
    private static final String KOTLIN_COMPILER_MODULE = "kotlin-compiler";
    private static final String KOTLIN_STDLIB_MODULE = "kotlin-stdlib";
    public static final String KOTLIN_HOME_LOCATION = OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/kotlin_home";

    private KotlinUtil() {}

    @Nullable
    static String getKotlinVersion(Project project) {
        return project.getBuildscript()
                .getConfigurations()
                .getByName("classpath")
                .getResolvedConfiguration()
                .getResolvedArtifacts()
                .stream()
                .filter(resolvedArtifact -> {
                    ModuleVersionIdentifier identifier = resolvedArtifact.getModuleVersion().getId();
                    return (KOTLIN_GROUP.equals(identifier.getGroup()) &&
                            KOTLIN_COMPILER_MODULE.equals(identifier.getName()));
                })
                .findFirst()
                .map(r -> r.getModuleVersion().getId().getVersion())
                .orElse(null);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void setupKotlinHome(Project rootProject) {
        String kotlinVersion = getKotlinVersion(rootProject);

        Configuration kotlinConfig = rootProject.getConfigurations().maybeCreate(KOTLIN_DEPS_CONFIG);
        DependencyHandler handler = rootProject.getDependencies();
        handler.add(KOTLIN_DEPS_CONFIG, String.format("%s:%s:%s", KOTLIN_GROUP, KOTLIN_COMPILER_MODULE, kotlinVersion));
        handler.add(KOTLIN_DEPS_CONFIG, String.format("%s:%s:%s", KOTLIN_GROUP, KOTLIN_STDLIB_MODULE, kotlinVersion));

        new DependencyCache("kotlin",
                rootProject,
                KOTLIN_HOME_LOCATION,
                Collections.singleton(kotlinConfig),
                null);
    }
}
