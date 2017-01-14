package com.uber.okbuck.core.model.java;

import org.gradle.api.Project;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class JavaAppTarget extends JavaLibTarget {

    public JavaAppTarget(Project project, String name) {
        super(project, name);
    }

    @Nullable
    public String getMainClass() {
        Object mainClass = getProject().getProperties().get("mainClassName");
        return mainClass == null ? null : mainClass.toString();
    }

    public Set<String> getExcludes() {
        return ((Jar) getProject().getTasks().getByName("jar")).getExcludes();
    }
}
