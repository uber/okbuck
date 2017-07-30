package com.uber.okbuck.core.model.scala

import com.uber.okbuck.core.model.base.Scope
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
    Scope getMain() {
        return new Scope(project,
                compileConfigs,
                (project.sourceSets.main.java.srcDirs as Set) + (project.sourceSets.main.scala.srcDirs as Set),
                project.file("src/main/resources"),
                Collections.emptyList())
    }

    @Override
    Scope getTest() {
        return new Scope(project,
                expand(compileConfigs, TEST_PREFIX),
                (project.sourceSets.test.java.srcDirs as Set) + (project.sourceSets.test.scala.srcDirs as Set),
                project.file("src/test/resources"),
                Collections.emptyList())
    }
}
