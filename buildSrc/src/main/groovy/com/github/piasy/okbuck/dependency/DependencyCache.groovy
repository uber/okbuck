package com.github.piasy.okbuck.dependency

import com.github.piasy.okbuck.util.FileUtil
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

import java.security.MessageDigest

class DependencyCache {

    final Project rootProject
    final File cacheDir
    private Map<VersionlessDependency, String> finalDepFiles = [:]
    private Map<VersionlessDependency, ExternalDependency> greatestVersions = [:]

    DependencyCache(Project rootProject) {
        this.rootProject = rootProject
        cacheDir = new File(rootProject.projectDir, ".okbuck/cache")
        cacheDir.mkdirs()
    }

    void put(ExternalDependency dependency) {
        if (!isValid(dependency.depFile)) {
            throw new IllegalArgumentException("${dependency.depFile.absolutePath} is not a valid jar/aar file")
        }

        ExternalDependency externalDependency = greatestVersions.get(dependency)
        if (externalDependency == null || dependency.version.compareTo(externalDependency.version) > 0) {
            greatestVersions.put(dependency, dependency)
        }
    }

    String get(ExternalDependency dependency) {
        ExternalDependency greatestVersion = greatestVersions.get(dependency)
        if (!finalDepFiles.containsKey(greatestVersion)) {
            File depFile = greatestVersion.depFile
            File cachedCopy = new File(cacheDir, "${md5(depFile.parentFile.absolutePath)}/${depFile.name}")
            if (!cachedCopy.exists()) {
                FileUtils.copyFile(depFile, cachedCopy)
            }
            String path = FileUtil.getRelativePath(rootProject.projectDir, cachedCopy)
            finalDepFiles.put(greatestVersion, path)

            File thirdPartyBuckFile = new File(cachedCopy.parentFile, "BUCK")
            if (!thirdPartyBuckFile.exists()) {
                copyThirdPartyBuckFile(cachedCopy.parentFile)
            }
            return path
        } else {
            return finalDepFiles.get(dependency)
        }
    }

    private static void copyThirdPartyBuckFile(File dstDir) {
        FileUtil.copyResourceToProject("thirdparty/BUCK_FILE", new File(dstDir, "BUCK"))
    }

    private static boolean isValid(File dep) {
        return (dep.name.endsWith(".jar") || dep.name.endsWith(".aar"))
    }

    static String md5(String s) {
        MessageDigest digest = MessageDigest.getInstance("MD5")
        digest.update(s.bytes);
        new BigInteger(1, digest.digest()).toString(16).padLeft(32, '0')
    }
}
