package com.uber.okbuck.core.dependency

import org.apache.commons.io.FilenameUtils
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.ModuleVersionIdentifier


class ExternalDependency extends VersionlessDependency {

    static final String LOCAL_DEP_VERSION = "1.0.0"
    static final String SOURCES_JAR = '-sources.jar'

    final DefaultArtifactVersion version
    final File depFile

    ExternalDependency(ModuleVersionIdentifier identifier, File depFile) {
        super(identifier)
        if (identifier.version) {
            version = new DefaultArtifactVersion(identifier.version)
        } else {
            version = new DefaultArtifactVersion(LOCAL_DEP_VERSION)
        }

        this.depFile = depFile
    }

    @Override
    String toString() {
        return "${this.version} : ${this.depFile.toString()}"
    }

    String getCacheName(boolean useFullDepName = false) {
        if (useFullDepName) {
            if (group) {
                return "${group}.${depFile.name}"
            } else {
                return "${name}.${depFile.name}"
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
        ModuleVersionIdentifier identifier = getDepIdentifier(
                baseName,
                baseName,
                LOCAL_DEP_VERSION)
        return new ExternalDependency(identifier, localDep)
    }

    static ModuleVersionIdentifier getDepIdentifier(String group, String name, String version) {
        return new ModuleVersionIdentifier() {

            @Override
            String getVersion() {
                return version
            }

            @Override
            String getGroup() {
                return group
            }

            @Override
            String getName() {
                return name
            }

            @Override
            ModuleIdentifier getModule() {
                return null
            }
        }
    }
}
