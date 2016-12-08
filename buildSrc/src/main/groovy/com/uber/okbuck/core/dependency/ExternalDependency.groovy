package com.uber.okbuck.core.dependency

import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.gradle.api.artifacts.ModuleVersionIdentifier


class ExternalDependency extends VersionlessDependency {

    static final String SOURCES_JAR = '-sources.jar'
    final DefaultArtifactVersion version
    final File depFile

    ExternalDependency(ModuleVersionIdentifier identifier, File depFile) {
        super(identifier)
        if (identifier.version) {
            version = new DefaultArtifactVersion(identifier.version)
        } else {
            version = new DefaultArtifactVersion("1.0.0")
        }
        this.depFile = depFile
    }

    @Override
    String toString() {
        return "${this.version} : ${this.depFile.toString()}"
    }

    String getCacheName(boolean useFullDepName=false) {
        if(useFullDepName) {
            String extension = depFile.name.substring(depFile.name.lastIndexOf('.'))
            return "${group}__${name}__${version}${extension}"
        } else {
            return depFile.name
        }
    }

    String getSourceCacheName(boolean useFullDepName=false) {
        if (useFullDepName) {
            return "${group}__${name}__${version}${SOURCES_JAR}"
        } else {
            return depFile.name.replaceFirst(/\.(jar|aar)$/, SOURCES_JAR)
        }
    }
}
