package com.uber.okbuck.core.model.kotlin

import com.uber.okbuck.core.model.base.Scope
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
    Scope getMain() {
        return new Scope(project,
                compileConfigs,
                (project.sourceSets.main.java.srcDirs as Set) + (project.sourceSets.main.kotlin.srcDirs as Set),
                project.file("src/main/resources"),
                Collections.emptyList())
    }

    @Override
    Scope getTest() {
        return new Scope(project,
                expand(compileConfigs, TEST_PREFIX, true),
                (project.sourceSets.test.java.srcDirs as Set) + (project.sourceSets.test.kotlin.srcDirs as Set),
                project.file("src/test/resources"),
                Collections.emptyList())
    }
}
