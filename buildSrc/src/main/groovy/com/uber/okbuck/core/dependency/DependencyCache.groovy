package com.uber.okbuck.core.dependency

import com.uber.okbuck.core.util.FileUtil
import groovy.transform.Synchronized
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path


class DependencyCache {

    static final String THIRD_PARTY_BUCK_FILE = "thirdparty/BUCK_FILE"
    final Project rootProject
    final File cacheDir

    final boolean useFullDepName
    final boolean useGreatestVersion
    final boolean fetchSources
    final boolean extractLintJars

    private Map<VersionlessDependency, String> lintJars = [:]
    private Map<VersionlessDependency, ExternalDependency> greatestVersions = new HashMap<>()

    DependencyCache(Project rootProject,
                    String cacheDirPath,
                    String buckFile = null,
                    boolean useFullDepName = false,
                    boolean useGreatestVersion = false,
                    boolean fetchSources = false,
                    boolean extractLintJars = false) {

        this.rootProject = rootProject

        this.cacheDir = new File(rootProject.projectDir, cacheDirPath)
        this.cacheDir.mkdirs()

        if (buckFile) {
            FileUtil.copyResourceToProject(buckFile, new File(cacheDir, "BUCK"))
        }

        this.useFullDepName = useFullDepName
        this.useGreatestVersion = useGreatestVersion
        this.fetchSources = fetchSources
        this.extractLintJars = extractLintJars
    }

    @Synchronized
    void put(ExternalDependency dependency) {
        if (!isValid(dependency.depFile)) {
            throw new InValidDependencyException("${dependency.depFile.absolutePath} is not a valid dependency")
        }

        ExternalDependency externalDependency = greatestVersions.get(dependency)
        if (externalDependency == null || dependency.version > externalDependency.version) {
            greatestVersions.put(dependency, dependency)
        }
    }

    String get(ExternalDependency dependency) {
        File cachedCopy = new File(cacheDir, dependency.getCacheName(useFullDepName))
        String path = FileUtil.getRelativePath(rootProject.projectDir, cachedCopy)

        if (useGreatestVersion) {
            String extension = path.substring(path.lastIndexOf('.'))
            path = path.substring(0, path.lastIndexOf('__')) + extension
        }

        return path
    }

    void finalizeCache() {

        // Delete files from cache which are not needed
        if (useGreatestVersion) {
            HashSet<String> greatestVersionCacheNames = new HashSet<>()
            for (Map.Entry<VersionlessDependency, ExternalDependency> entry : greatestVersions.entrySet()) {
                greatestVersionCacheNames.add(entry.getValue().getCacheName(useFullDepName))
                greatestVersionCacheNames.add(entry.getValue().getSourceCacheName(useFullDepName))
            }

            for (File cacheDirFile : cacheDir.listFiles()) {
                if (isValid(cacheDirFile) && !greatestVersionCacheNames.contains(cacheDirFile.name)) {
                    cacheDirFile.delete()
                }
            }
        }

        // Run dependency related tasks
        for (Map.Entry<VersionlessDependency, ExternalDependency> entry : greatestVersions.entrySet()) {
            File cachedCopy = new File(cacheDir, entry.getValue().getCacheName(useFullDepName))

            // Copy the file into the cache
            if (!cachedCopy.exists()) {
                Files.copy(entry.getValue().depFile.toPath(), cachedCopy.toPath())
            }

            // Extract Lint Jars
            if (extractLintJars && cachedCopy.name.endsWith(".aar")) {
                File lintJar = getPackagedLintJar(cachedCopy)
                if (lintJar != null) {
                    String lintJarPath = FileUtil.getRelativePath(rootProject.projectDir, lintJar)
                    lintJars.put(entry.getValue(), lintJarPath)
                }
            }

            // Fetch Sources
            if (fetchSources) {
                fetchSourcesFor(entry.getValue())
            }
        }
    }

    String getLintJar(ExternalDependency dependency) {
        return lintJars.get(dependency)
    }

    private void fetchSourcesFor(ExternalDependency dependency) {
        String sourcesJarName = dependency.depFile.name.replaceFirst(/\.(jar|aar)$/, ExternalDependency.SOURCES_JAR)

        File sourcesJar = null
        if (FileUtils.directoryContains(rootProject.projectDir, dependency.depFile)) {
            sourcesJar = new File(dependency.depFile.parentFile, sourcesJarName)
        } else {
            def sourceJars = rootProject.fileTree(
                    dir: dependency.depFile.parentFile.parentFile.absolutePath,
                    includes: ["**/${sourcesJarName}"]) as List
            if (sourceJars.size() > 0) {
                sourcesJar = sourceJars[0]
            }
        }

        File cachedCopy = new File(cacheDir, dependency.getSourceCacheName(useFullDepName))
        if (sourcesJar != null && sourcesJar.exists() && !cachedCopy.exists()) {
            FileUtils.copyFile(sourcesJar, cachedCopy)
        }
    }

    boolean isValid(File dep) {
        return (dep.name.endsWith(".jar") || dep.name.endsWith(".aar"))
    }

    static File getPackagedLintJar(File aar) {
        File lintJar = new File(aar.parentFile, aar.name.replaceFirst(/\.aar$/, '-lint.jar'))
        if (lintJar.exists()) {
            return lintJar
        }
        FileSystem zipFile = FileSystems.newFileSystem(aar.toPath(), null)
        Path packagedLintJar = zipFile.getPath("lint.jar")
        if (Files.exists(packagedLintJar)) {
            Files.copy(packagedLintJar, lintJar.toPath())
            return lintJar
        } else {
            return null
        }
    }
}
