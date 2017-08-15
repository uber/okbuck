package com.uber.okbuck.core.model.groovy

import com.uber.okbuck.core.model.java.JavaLibTarget
import org.gradle.api.Project

class GroovyLibTarget extends JavaLibTarget {

    GroovyLibTarget(Project project, String name) {
        super(project, name)
    }

    @Override
    protected Set<File> getMainSrcDirs() {
        return (project.sourceSets.main.java.srcDirs as Set) + (project.sourceSets.main.groovy.srcDirs as Set)
    }

    @Override
    protected Set<File> getTestSrcDirs() {
        return (project.sourceSets.test.java.srcDirs as Set) + (project.sourceSets.test.groovy.srcDirs as Set)
    }
}
