package com.uber.okbuck.core.dependency

import com.uber.okbuck.core.util.FileUtil
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.plugins.ide.internal.IdeDependenciesExtractor

final class DependencyUtils {

    // These are used by conventions such as gradleApi() and localGroovy() and are whitelisted
    private static final Set<String> WHITELIST_LOCAL_PATTERNS = ['generated-gradle-jars/gradle-api-', 'wrapper/dists']

    private DependencyUtils() {}

    static Set<Configuration> useful(Set<Configuration> configurations) {
        Set<Configuration> useful = configurations.findAll { Configuration configuration ->
            !configuration.dependencies.empty
        }
        useful.removeAll(useful.collect { it.extendsFrom }.flatten())
        return useful
    }

    static File createCacheDir(Project project, String cacheDirPath, String buckFile = null) {
        File cacheDir = new File(project.rootProject.projectDir, cacheDirPath)
        cacheDir.mkdirs()

        if (buckFile) {
            FileUtil.copyResourceToProject(buckFile, new File(cacheDir, "BUCK"))
        }
        return cacheDir
    }

    static void downloadSourceJars(Project project, Set<Configuration> configurations) {
        new IdeDependenciesExtractor().extractRepoFileDependencies(
                project.dependencies,
                configurations,
                [],
                true,
                false)
    }

    static boolean isWhiteListed(File depFile) {
        return WHITELIST_LOCAL_PATTERNS.find { depFile.absolutePath.contains(it) } != null
    }

    static boolean isConsumable(File file) {
        return file.name.endsWith(".jar") || file.name.endsWith(".aar")
    }

    static void validate(Project project, ExternalDependency dependency) {
        String version = dependency.version.toString()
        if (version.contains("+") || version.contains("-SNAPSHOT")) {
            String message =
                    "${dependency.cacheName} : Please do not use changing dependencies. They can cause hard " +
                            "to reproduce builds"
            if (project.rootProject.okbuck.failOnChangingDependencies) {
                throw new IllegalStateException(message)
            } else {
                println "\n${message}\n"
            }
        }
    }
}
