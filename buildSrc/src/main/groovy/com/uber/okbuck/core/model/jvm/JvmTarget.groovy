package com.uber.okbuck.core.model.jvm

import com.uber.okbuck.core.model.base.Scope
import com.uber.okbuck.core.model.base.Target
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

abstract class JvmTarget extends Target {

    public static final String MAIN = "main"

    JvmTarget(Project project, String name) {
        super(project, name)
    }

    /**
     * Main Scope
     */
    abstract Scope getMain()

    /**
     * Test Scope
     */
    abstract Scope getTest()

    /**
     * List of test jvm args
     */
    List<String> getTestRunnerJvmArgs() {
        Test testTask = project.tasks.withType(Test).find {
            it.name == "test"
        }
        return testTask != null ? testTask.allJvmArgs : []
    }
}
