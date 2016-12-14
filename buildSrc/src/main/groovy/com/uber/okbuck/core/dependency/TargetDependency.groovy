package com.uber.okbuck.core.dependency

import org.gradle.api.artifacts.ModuleVersionIdentifier


class TargetDependency extends VersionlessDependency {

    final String projectIdentifier

    TargetDependency(ModuleVersionIdentifier identifier, String projectIdentifier) {
        super(identifier)
        this.projectIdentifier = projectIdentifier
    }

    @Override
    String toString() {
        return "${this.group}:${this.name} -> ${this.projectIdentifier}"
    }
}
