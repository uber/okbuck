package com.uber.okbuck.core.util;

import com.google.common.collect.ImmutableMap;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;

import org.apache.commons.lang3.tuple.Pair;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;

public final class KotlinUtil {

    private static final String KOTLIN_DEPS_CONFIG = "okbuck_kotlin_deps";
    private static final String KOTLIN_GROUP = "org.jetbrains.kotlin";
    private static final String KOTLIN_COMPILER_MODULE = "kotlin-compiler-embeddable";
    private static final String KOTLIN_RUNTIME_MODULE = "kotlin-runtime";
    private static final String KOTLIN_HOME_LOCATION = OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/kotlin_installation";

    private KotlinUtil() {}

    @Nullable
    public static String getKotlinVersion(Project project) {
        return project.getBuildscript()
                .getConfigurations()
                .getByName("classpath")
                .getResolvedConfiguration()
                .getResolvedArtifacts()
                .parallelStream()
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
    public static Pair<String, String> setupKotlinHome(Project rootProject) {
        String kotlinVersion = getKotlinVersion(rootProject);

        Configuration kotlinConfig = rootProject.getConfigurations().maybeCreate(KOTLIN_DEPS_CONFIG);
        DependencyHandler handler = rootProject.getDependencies();
        handler.add(KOTLIN_DEPS_CONFIG, String.format("%s:%s:%s", KOTLIN_GROUP, KOTLIN_COMPILER_MODULE, kotlinVersion));
        handler.add(KOTLIN_DEPS_CONFIG, String.format("%s:%s:%s", KOTLIN_GROUP, KOTLIN_RUNTIME_MODULE, kotlinVersion));

        new DependencyCache("kotlin",
                rootProject,
                KOTLIN_HOME_LOCATION + "/lib",
                Collections.singleton(kotlinConfig),
                null);

        File kotlinHome = new File(KOTLIN_HOME_LOCATION);

        File kotlinc = new File(kotlinHome, "bin/kotlinc");
        FileUtil.copyResourceToProject("kotlin/bin/kotlinc",
                new File(kotlinHome, "bin/kotlinc"),
                ImmutableMap.of("template-kotlin-version", kotlinVersion));
        kotlinc.setExecutable(true);

        String kotlinCompiler = rootProject.relativePath(kotlinc);
        String kotlinRuntime = String.format("%s/lib/%s-%s.jar", KOTLIN_HOME_LOCATION, KOTLIN_RUNTIME_MODULE,
                kotlinVersion);
        return Pair.of(kotlinCompiler, kotlinRuntime);
    }
}
