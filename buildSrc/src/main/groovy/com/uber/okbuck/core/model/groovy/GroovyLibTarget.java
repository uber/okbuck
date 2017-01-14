package com.uber.okbuck.core.model.groovy;

import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.core.model.java.JavaLibTarget;

import org.gradle.api.Project;
import org.gradle.api.tasks.compile.JavaCompile;

import java.util.Collections;

public class GroovyLibTarget extends JavaLibTarget {

    public GroovyLibTarget(Project project, String name) {
        super(project, name);
    }

    @Override
    public Scope getMain() {
        return new Scope(
                getProject(),
                Collections.singleton("compile"),
                getProject().files("src/main/java", "src/main/groovy").getFiles(),
                getProject().file("src/main/resources"),
                ((JavaCompile) getProject().getTasks().getByName("compileJava")).getOptions().getCompilerArgs());
    }

    @Override
    public Scope getTest() {
        return new Scope(
                getProject(),
                Collections.singleton("testCompile"),
                getProject().files("src/test/java", "src/test/groovy").getFiles(),
                getProject().file("src/test/resources"),
                ((JavaCompile) getProject().getTasks().getByName("compileTestJava")).getOptions().getCompilerArgs());
    }
}
