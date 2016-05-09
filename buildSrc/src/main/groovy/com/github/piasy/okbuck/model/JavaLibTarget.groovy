package com.github.piasy.okbuck.model

import org.gradle.api.Project

/**
 * A java library target
 */
class JavaLibTarget extends Target {

    static final String MAIN = "main"

    JavaLibTarget(Project project, String name) {
        super(project, name)
    }

    @Override
    protected Set<File> sourceDirs() {
        return project.files("src/main/java") as Set
    }

    @Override
    protected Set<String> compileConfigurations() {
        return ["compile"]
    }
}
