package com.uber.okbuck.core.model.jvm;

import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.core.model.base.Target;

import org.gradle.api.Project;
import org.gradle.api.tasks.testing.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
     * The test options
     */
    public TestOptions getTestOptions() {
        try {
            Test testTask = getProject().getTasks().withType(Test.class).getByName("test");
            List<String> jvmArgs = testTask != null ? testTask.getAllJvmArgs() : Collections.<String>emptyList();
            Map<String, Object> env = testTask != null ? testTask.getEnvironment() : Collections.emptyMap();
            env.keySet().removeAll(System.getenv().keySet());
            return new TestOptions(jvmArgs, env);
        } catch (Exception e) {
            return TestOptions.EMPTY;
        }
    }
}
