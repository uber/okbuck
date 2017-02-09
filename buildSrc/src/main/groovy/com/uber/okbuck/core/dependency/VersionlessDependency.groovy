package com.uber.okbuck.core.dependency

import groovy.transform.EqualsAndHashCode
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.ModuleVersionIdentifier

@EqualsAndHashCode
class VersionlessDependency {

    final String group
    final String name
    final String classifier

    VersionlessDependency(ModuleVersionIdentifier identifier, String artifactClassifier) {
        group = identifier.group
        name = identifier.name
        if (classifier) {
            classifier = artifactClassifier
        } else {
            classifier = "debug"
        }
    }

    @Override
    String toString() {
        if (classifier) {
            return "${this.group}:${this.name}-${this.classifier}"
        }
        return "${this.group}:${this.name}"
    }

    static ModuleVersionIdentifier getDepIdentifier(String group, String name, String version = null) {
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
