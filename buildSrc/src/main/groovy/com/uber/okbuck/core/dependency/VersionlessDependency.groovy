package com.uber.okbuck.core.dependency

import groovy.transform.EqualsAndHashCode
import org.gradle.api.artifacts.ModuleVersionIdentifier

@EqualsAndHashCode
class VersionlessDependency {

    final String group
    final String name

    VersionlessDependency(ModuleVersionIdentifier identifier) {
        group = identifier.group
        name = identifier.name
    }
}
