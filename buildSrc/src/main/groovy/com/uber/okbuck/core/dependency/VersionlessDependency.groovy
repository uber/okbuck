package com.uber.okbuck.core.dependency

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class VersionlessDependency {

    final String group
    final String name
    final boolean isLocal

    VersionlessDependency(String group, String name) {
        this(group, name, false)
    }

    protected VersionlessDependency(String group, String name, boolean isLocal) {
        this.group = group
        this.name = name
        this.isLocal = isLocal
    }

    @Override
    String toString() {
        return "${this.group}:${this.name}"
    }
}
