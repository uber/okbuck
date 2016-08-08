package com.github.okbuilds.core.dependency

import com.github.okbuilds.core.util.FileUtil
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

class DependencyCache {

    final Project rootProject
    final File cacheDir
    final boolean useFullDepName
    final boolean fetchSources

    private Map<VersionlessDependency, String> finalDepFiles = [:]
    private Map<VersionlessDependency, ExternalDependency> greatestVersions = [:]

    DependencyCache(Project rootProject,
                    String cacheDirPath,
                    useFullDepName = false,
                    createBuckFile = true,
                    fetchSources = false) {
        this.rootProject = rootProject
        this.useFullDepName = useFullDepName
        this.fetchSources = fetchSources
        cacheDir = new File(rootProject.projectDir, cacheDirPath)
        cacheDir.mkdirs()
        if (createBuckFile) {
            FileUtil.copyResourceToProject("thirdparty/BUCK_FILE", new File(cacheDir, "BUCK"))
        }
    }

    void put(ExternalDependency dependency) {
        if (!isValid(dependency.depFile)) {
            throw new IllegalArgumentException("${dependency.depFile.absolutePath} is not a valid jar/aar file")
        }

        ExternalDependency externalDependency = greatestVersions.get(dependency)
        if (externalDependency == null || dependency.version > externalDependency.version) {
            greatestVersions.put(dependency, dependency)
        }
    }

    String get(ExternalDependency dependency) {
        ExternalDependency greatestVersion = greatestVersions.get(dependency)
        if (!finalDepFiles.containsKey(greatestVersion)) {
            File depFile = greatestVersion.depFile
            File cachedCopy = new File(cacheDir, useFullDepName ? dependency.cacheName : dependency.depFile.name)
            if (!cachedCopy.exists()) {
                FileUtils.copyFile(depFile, cachedCopy)
            }
            if (fetchSources) {
                fetchSourcesFor(dependency)
            }
            String path = FileUtil.getRelativePath(rootProject.projectDir, cachedCopy)
            finalDepFiles.put(greatestVersion, path)
        }

        return finalDepFiles.get(greatestVersion)
    }

    private void fetchSourcesFor(ExternalDependency dependency) {
        File depFile = dependency.depFile
        String sourcesJarName = depFile.name.replaceFirst(/\.(jar|aar)$/, '-sources.jar')

        File sourcesJar = null
        if (FileUtils.directoryContains(rootProject.projectDir, dependency.depFile)) {
            sourcesJar = new File(depFile.parentFile, sourcesJarName)
        } else {
            def sourceJars = rootProject.fileTree(dir: depFile.parentFile.parentFile.absolutePath,
                    includes: ["**/${sourcesJarName}"]) as List
            if (sourceJars.size() > 0) {
                sourcesJar = sourceJars[0]
            }
        }

        File cachedCopy = new File(cacheDir, "${dependency.group}.${sourcesJarName}")
        if (sourcesJar != null && sourcesJar.exists() && !cachedCopy.exists()) {
            FileUtils.copyFile(sourcesJar, cachedCopy)
        }
    }

    private static boolean isValid(File dep) {
        return (dep.name.endsWith(".jar") || dep.name.endsWith(".aar"))
    }
}
