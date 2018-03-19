package com.uber.okbuck.core.model.kotlin

import com.uber.okbuck.core.model.base.Scope
import com.uber.okbuck.core.model.java.JavaLibTarget
import org.gradle.api.Project

/**
 * A kotlin library target
 */
class KotlinLibTarget extends JavaLibTarget {

    public static final String ANNOTATION_PROCESSOR_CONFIGURATION_NAME = "kapt"

    KotlinLibTarget(Project project, String name) {
        super(project, name)
    }

    @Override
    protected Set<File> getMainSrcDirs() {
        return (project.sourceSets.main.java.srcDirs as Set) + (project.sourceSets.main.kotlin.srcDirs as Set)
    }

    @Override
    protected Set<File> getTestSrcDirs() {
        return (project.sourceSets.test.java.srcDirs as Set) + (project.sourceSets.test.kotlin.srcDirs as Set)
    }

    @Override
    Scope getApt() {
        return Scope.from(project, ANNOTATION_PROCESSOR_CONFIGURATION_NAME)
    }

    @Override
    Scope getTestApt() {
        return Scope.from(project, ANNOTATION_PROCESSOR_CONFIGURATION_NAME)
    }
}
