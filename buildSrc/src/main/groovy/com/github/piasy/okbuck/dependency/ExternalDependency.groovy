package com.github.piasy.okbuck.dependency

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.apache.maven.artifact.versioning.DefaultArtifactVersion

@ToString(includes = ['version', 'depFile'])
@EqualsAndHashCode(includes = ["depFile"])
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
