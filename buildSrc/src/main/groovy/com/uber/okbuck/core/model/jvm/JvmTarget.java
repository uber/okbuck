package com.uber.okbuck.core.model.jvm;

import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.core.model.base.Target;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;

import java.util.Collections;
import java.util.List;

public abstract class JvmTarget extends Target {

    public static final String MAIN = "main";

    public JvmTarget(Project project, String name) {
        super(project, name);
    }

    /**
     * Main Scope
     */
    public abstract Scope getMain();

    /**
     * Test Scope
     */
    public abstract Scope getTest();

    /**
     * List of test jvm args
     */
    public List<String> getTestRunnerJvmArgs() {
        try {
            Test testTask = getProject().getTasks().withType(Test.class).getByName("test");
            return testTask != null ? testTask.getAllJvmArgs() : Collections.<String>emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
