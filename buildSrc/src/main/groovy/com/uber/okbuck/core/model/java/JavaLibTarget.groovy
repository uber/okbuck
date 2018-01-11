package com.uber.okbuck.core.model.java

import com.uber.okbuck.core.model.base.Scope
import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.jvm.tasks.Jar
import org.jetbrains.annotations.Nullable

/**
 * A java library target
 */
class JavaLibTarget extends JavaTarget {

    JavaLibTarget(Project project, String name) {
        super(project, name)
    }

    protected Set<File> getMainSrcDirs() {
        return project.sourceSets.main.java.srcDirs as Set
    }

    protected Set<File> getTestSrcDirs() {
        return project.sourceSets.test.java.srcDirs as Set
    }

    @Override
    Scope getMain() {
        return Scope.from(project,
                depsConfig,
                mainSrcDirs,
                project.file("src/main/resources"),
                project.compileJava.options.compilerArgs as List)
    }

    @Override
    Scope getTest() {
        return Scope.from(project,
                expand(depsConfig, UNIT_TEST_PREFIX),
                testSrcDirs,
                project.file("src/test/resources"),
                project.compileTestJava.options.compilerArgs as List)
    }

    String getSourceCompatibility() {
        return javaVersion(project.sourceCompatibility)
    }

    String getTargetCompatibility() {
        return javaVersion(project.targetCompatibility)
    }

    boolean hasApplication() {
        return project.plugins.hasPlugin(ApplicationPlugin)
    }

    @Nullable
    String getMainClass() {
        Object mainClass = getProject().getProperties().get("mainClassName")
        return mainClass == null ? null : mainClass.toString()
    }

    Set<String> getExcludes() {
        return ((Jar) getProject().getTasks().getByName("jar")).getExcludes()
    }
}
