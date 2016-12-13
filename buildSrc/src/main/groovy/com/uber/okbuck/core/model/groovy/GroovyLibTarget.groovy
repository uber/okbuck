package com.uber.okbuck.core.model.groovy

import com.uber.okbuck.core.model.base.Scope
import com.uber.okbuck.core.model.java.JavaLibTarget
import org.gradle.api.Project

class GroovyLibTarget extends JavaLibTarget {

    GroovyLibTarget(Project project, String name) {
        super(project, name)
    }

    @Override
    Scope getMain() {
        return new Scope(project,
                ["compile"],
                project.files("src/main/java", "src/main/groovy") as Set,
                project.file("src/main/resources"),
                project.compileJava.options.compilerArgs as List)
    }

    @Override
    Scope getTest() {
        return new Scope(project,
                ["testCompile"],
                project.files("src/test/java", "src/test/groovy") as Set,
                project.file("src/test/resources"),
                project.compileTestJava.options.compilerArgs as List)
    }
}
