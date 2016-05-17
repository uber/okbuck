package com.github.okbuilds.core.dependency

import org.apache.maven.artifact.versioning.DefaultArtifactVersion

class ExternalDependency extends VersionlessDependency {

    final DefaultArtifactVersion version
    final File depFile

    ExternalDependency(String identifier, File depFile) {
        super(identifier)
        List<String> parts = identifier.split(":")
        version = new DefaultArtifactVersion(parts[2])
        this.depFile = depFile
    }
}
