package com.uber.okbuck.core.model.kotlin

import com.uber.okbuck.core.model.java.JavaLibTarget
import org.gradle.api.Project

/**
 * A kotlin library target
 */
class KotlinLibTarget extends JavaLibTarget {

    KotlinLibTarget(Project project, String name) {
        super(project, name)
    }

    @Override
    protected Set<File> getMainSrcDirs() {
        return (project.sourceSets.main.java.srcDirs as Set) + (project.sourceSets.main.kotlin.srcDirs as Set)
    }

    @Override
    protected Set<File> getTestSrcDirs() {
        return (project.sourceSets.test.java.srcDirs as Set) + (project.sourceSets.test.kotlin.srcDirs as Set)
    }
}
