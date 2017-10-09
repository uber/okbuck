package com.uber.okbuck.core.dependency

import com.uber.okbuck.core.util.FileUtil
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.UnknownConfigurationException
import org.gradle.plugins.ide.internal.IdeDependenciesExtractor

final class DependencyUtils {

    // These are used by conventions such as gradleApi() and localGroovy() and are whitelisted
    private static final Set<String> WHITELIST_LOCAL_PATTERNS = ['generated-gradle-jars/gradle-api-', 'wrapper/dists']

    private DependencyUtils() {}

    static Set<Configuration> useful(Project project, Set<String> configurations) {
        Set<Configuration> useful = new HashSet<>()
        configurations.each { String configName ->
            try {
                useful.add(project.configurations.getByName(configName))
            } catch (UnknownConfigurationException ignored) {}
        }

        useful.findAll { Configuration configuration ->
            !configuration.dependencies.empty
        }
        useful.removeAll(useful.collect { it.extendsFrom }.flatten())
        return useful
    }

    static File createCacheDir(Project project, String cacheDirPath, String buckFile = null) {
        File cacheDir = project.rootProject.file(cacheDirPath)
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
}
