package com.uber.okbuck.core.util;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.DependencyUtils;

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

public final class KotlinUtil {

    private static final String KOTLIN_DEPS_CONFIG = "okbuck_kotlin_deps";
    private static final String KOTLIN_GROUP = "org.jetbrains.kotlin";
    private static final String KOTLIN_COMPILER_MODULE = "kotlin-compiler-embeddable";
    private static final String KOTLIN_GRADLE_MODULE = "kotlin-gradle-plugin";
    private static final String KOTLIN_STDLIB_MODULE = "kotlin-stdlib";
    public static final String KOTLIN_ANDROID_EXTENSIONS_MODULE = "kotlin-android-extensions";
    public static final String KOTLIN_HOME_LOCATION = OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/kotlin_home";

    private KotlinUtil() {}

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void setupKotlinHome(Project project) {
        String kotlinVersion = ProjectUtil.findVersionInClasspath(project, KOTLIN_GROUP, KOTLIN_GRADLE_MODULE);
        Configuration kotlinConfig = project.getConfigurations().maybeCreate(KOTLIN_DEPS_CONFIG);
        DependencyHandler handler = project.getDependencies();
        handler.add(KOTLIN_DEPS_CONFIG, String.format("%s:%s:%s", KOTLIN_GROUP, KOTLIN_COMPILER_MODULE, kotlinVersion));
        handler.add(KOTLIN_DEPS_CONFIG, String.format("%s:%s:%s", KOTLIN_GROUP, KOTLIN_STDLIB_MODULE, kotlinVersion));
        handler.add(KOTLIN_DEPS_CONFIG, String.format("%s:%s:%s", KOTLIN_GROUP, KOTLIN_ANDROID_EXTENSIONS_MODULE, kotlinVersion));

        new DependencyCache(project, DependencyUtils.createCacheDir(project, KOTLIN_HOME_LOCATION)).build(kotlinConfig);

        Path kotlinHome = project.file(KOTLIN_HOME_LOCATION).toPath();
        removeVersions(kotlinHome, KOTLIN_COMPILER_MODULE, "kotlin-compiler");
        removeVersions(kotlinHome, KOTLIN_STDLIB_MODULE);
        removeVersions(kotlinHome, KOTLIN_ANDROID_EXTENSIONS_MODULE);
    }

    private static void removeVersions(Path dir, String toRemove) {
        removeVersions(dir, toRemove, toRemove);
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

    public static boolean hasKotlinPluginInClasspath(Project project) {
        return ProjectUtil.findVersionInClasspath(project, KOTLIN_GROUP, KOTLIN_GRADLE_MODULE) != null;
    }
}
