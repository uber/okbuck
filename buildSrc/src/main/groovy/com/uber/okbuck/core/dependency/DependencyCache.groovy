package com.uber.okbuck.core.dependency

import com.uber.okbuck.core.util.FileUtil
import groovy.transform.Synchronized
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class DependencyCache {

    static final String THIRD_PARTY_BUCK_FILE = "thirdparty/BUCK_FILE"
    final Project rootProject
    final File cacheDir
    final boolean useFullDepName
    final boolean fetchSources
    final boolean extractLintJars

    private Map<VersionlessDependency, String> finalDepFiles = [:]
    private Map<VersionlessDependency, String> lintJars = [:]
    private Map<VersionlessDependency, ExternalDependency> greatestVersions = new ConcurrentHashMap()

    DependencyCache(Project rootProject,
                    String cacheDirPath,
                    boolean useFullDepName = false,
                    String buckFile = THIRD_PARTY_BUCK_FILE,
                    boolean fetchSources = false,
                    boolean extractLintJars = false) {
        this.rootProject = rootProject
        this.useFullDepName = useFullDepName
        this.fetchSources = fetchSources
        this.extractLintJars = extractLintJars
        cacheDir = new File(rootProject.projectDir, cacheDirPath)
        cacheDir.mkdirs()
        if (buckFile) {
            FileUtil.copyResourceToProject(buckFile, new File(cacheDir, "BUCK"))
        }
    }

    void put(ExternalDependency dependency) {
        if (!isValid(dependency.depFile)) {
            throw new InValidDependencyException("${dependency.depFile.absolutePath} is not a valid dependency")
        }

        ExternalDependency externalDependency = greatestVersions.get(dependency)
        if (externalDependency == null || dependency.version > externalDependency.version) {
            greatestVersions.put(dependency, dependency)
        }
    }

    @Synchronized
    String get(ExternalDependency dependency) {
        ExternalDependency greatestVersion = greatestVersions.get(dependency)
        if (!finalDepFiles.containsKey(greatestVersion)) {
            File depFile = greatestVersion.depFile
            File cachedCopy = new File(cacheDir, useFullDepName ? dependency.cacheName : dependency.depFile.name)
            if (!cachedCopy.exists()) {
                Files.copy(depFile.toPath(), cachedCopy.toPath())
            }
            if (extractLintJars && cachedCopy.name.endsWith(".aar")) {
                File lintJar = getPackagedLintJar(cachedCopy)
                if (lintJar != null) {
                    String lintJarPath = FileUtil.getRelativePath(rootProject.projectDir, lintJar)
                    lintJars.put(greatestVersion, lintJarPath)
                }
            }
            if (fetchSources) {
                fetchSourcesFor(dependency)
            }
            String path = FileUtil.getRelativePath(rootProject.projectDir, cachedCopy)
            finalDepFiles.put(greatestVersion, path)
        }

        return finalDepFiles.get(greatestVersion)
    }

    String getLintJar(ExternalDependency dependency) {
        return lintJars.get(dependency)
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
