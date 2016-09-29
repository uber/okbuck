package com.uber.okbuck.core.model

import org.gradle.api.Project

class JavaAppTarget extends JavaLibTarget {

    static final String MAIN = "main"

    JavaAppTarget(Project project, String name) {
        super(project, name);
    }

    String getMainClass() {
        return project.hasProperty('mainClassName') ? project.mainClassName : null
    }
}
