package com.github.okbuilds.core.dependency

import org.apache.maven.artifact.versioning.DefaultArtifactVersion

class ExternalDependency extends VersionlessDependency {

    final DefaultArtifactVersion version
    final File depFile

    ExternalDependency(String identifier, File depFile, String fallbackId = "") {
        super(identifier)
        List<String> parts = identifier.split(":")
        if (parts.size() < 3) {
            parts = fallbackId.split(":")
        }
        version = new DefaultArtifactVersion(parts[2])
        this.depFile = depFile
    }

    @Override
    String toString() {
        return "${this.version} : ${this.depFile.toString()}"
    }
}
