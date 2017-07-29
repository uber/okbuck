package com.uber.okbuck.core.model.groovy

import com.uber.okbuck.core.model.base.Scope
import com.uber.okbuck.core.model.java.JavaLibTarget
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

class GroovyLibTarget extends JavaLibTarget {

    GroovyLibTarget(Project project, String name) {
        super(project, name)
    }

    @Override
    Scope getMain() {
        return new Scope(
                getProject(),
                getCompileConfigs(),
                (project.sourceSets.main.java.srcDirs as Set) + (project.sourceSets.main.groovy.srcDirs as Set),
                getProject().file("src/main/resources"),
                ((JavaCompile) getProject().getTasks().getByName("compileJava")).getOptions().getCompilerArgs())
    }

    @Override
    Scope getTest() {
        return new Scope(
                getProject(),
                expand(getCompileConfigs(), TEST_PREFIX),
                (project.sourceSets.test.java.srcDirs as Set) + (project.sourceSets.test.groovy.srcDirs as Set),
                getProject().file("src/test/resources"),
                ((JavaCompile) getProject().getTasks().getByName("compileTestJava")).getOptions().getCompilerArgs())
    }
}
