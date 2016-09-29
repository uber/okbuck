package com.uber.okbuck.core.dependency

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class VersionlessDependency {

    final String group
    final String module

    VersionlessDependency(String identifier) {
        List<String> parts = identifier.split(":")
        group = parts[0]
        module = parts[1]
    }
}
