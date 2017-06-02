package com.uber.okbuck.core.dependency

import org.apache.commons.io.FilenameUtils
import org.apache.maven.artifact.versioning.DefaultArtifactVersion

class ExternalDependency extends VersionlessDependency {

    static final String LOCAL_DEP_VERSION = "1.0.0"
    static final String SOURCES_JAR = '-sources.jar'

    final DefaultArtifactVersion version
    final File depFile

    ExternalDependency(String group, String name, String version, File depFile) {
        this(group, name, version, depFile, false)
    }

    private ExternalDependency(String group, String name, String version, File depFile, boolean isLocal) {
        super(group, name, isLocal)
        if (version) {
            this.version = new DefaultArtifactVersion(version)
        } else {
            this.version = new DefaultArtifactVersion(LOCAL_DEP_VERSION)
        }

        this.depFile = depFile
    }

    @Override
    String toString() {
        return "${this.group}:${this.name}:${this.version} -> ${this.depFile.toString()}"
    }

    String getCacheName(boolean useFullDepName = false) {
        if (useFullDepName) {
            if (group) {
                return "${group}.${depFile.name}" as String
            } else {
                return "${name}.${depFile.name}" as String
            }
        } else {
            return depFile.name
        }
    }

    String getSourceCacheName(boolean useFullDepName = false) {
        return getCacheName(useFullDepName).replaceFirst(/\.(jar|aar)$/, SOURCES_JAR)
    }

    static ExternalDependency fromLocal(File localDep) {
        String baseName = FilenameUtils.getBaseName(localDep.name)
        return new ExternalDependency(baseName, baseName, LOCAL_DEP_VERSION, localDep, true)
    }
}
