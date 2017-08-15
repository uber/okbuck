package com.uber.okbuck.core.model.scala

import com.uber.okbuck.core.model.java.JavaLibTarget
import org.gradle.api.Project

/**
 * A scala library target
 */
class ScalaLibTarget extends JavaLibTarget {

    ScalaLibTarget(Project project, String name) {
        super(project, name)
    }

    @Override
    protected Set<File> getMainSrcDirs() {
        return (project.sourceSets.main.java.srcDirs as Set) + (project.sourceSets.main.scala.srcDirs as Set)
    }

    @Override
    protected Set<File> getTestSrcDirs() {
        return (project.sourceSets.test.java.srcDirs as Set) + (project.sourceSets.test.scala.srcDirs as Set)
    }
}
