package com.uber.okbuck.core.util;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;

public final class KotlinUtil {

    private static final String KOTLIN_DEPS_CONFIG = "okbuck_kotlin_deps";
    private static final String KOTLIN_GROUP = "org.jetbrains.kotlin";
    private static final String KOTLIN_COMPILER_MODULE = "kotlin-compiler-embeddable";
    private static final String KOTLIN_GRADLE_MODULE = "kotlin-gradle-plugin";
    private static final String KOTLIN_STDLIB_MODULE = "kotlin-stdlib";
    public static final String KOTLIN_HOME_LOCATION = OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/kotlin_home";

    private KotlinUtil() {}

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void setupKotlinHome(Project rootProject) {
        String kotlinVersion = ProjectUtil.findVersionInClasspath(rootProject, KOTLIN_GROUP, KOTLIN_GRADLE_MODULE);
        Configuration kotlinConfig = rootProject.getConfigurations().maybeCreate(KOTLIN_DEPS_CONFIG);
        DependencyHandler handler = rootProject.getDependencies();
        handler.add(KOTLIN_DEPS_CONFIG, String.format("%s:%s:%s", KOTLIN_GROUP, KOTLIN_COMPILER_MODULE, kotlinVersion));
        handler.add(KOTLIN_DEPS_CONFIG, String.format("%s:%s:%s", KOTLIN_GROUP, KOTLIN_STDLIB_MODULE, kotlinVersion));

        new DependencyCache("kotlin",
                rootProject,
                KOTLIN_HOME_LOCATION,
                Collections.singleton(kotlinConfig),
                null);

        removeVersions(Paths.get(KOTLIN_HOME_LOCATION),
                KOTLIN_COMPILER_MODULE, "kotlin-compiler");
        removeVersions(Paths.get(KOTLIN_HOME_LOCATION),
                KOTLIN_STDLIB_MODULE, KOTLIN_STDLIB_MODULE);
    }

    private static void removeVersions(Path dir, String toRename, String renamed) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws
                        IOException {
                    String fileName = file.getFileName().toString();
                    if (fileName.startsWith(toRename)) {
                        Files.move(file, file.getParent().resolve(renamed + ".jar"));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {}
    }
}
