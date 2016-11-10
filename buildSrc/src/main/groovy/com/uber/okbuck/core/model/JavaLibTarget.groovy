package com.uber.okbuck.core.model

import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.util.ProjectUtil
import org.gradle.api.Project

/**
 * A java library target
 */
class JavaLibTarget extends JavaTarget {

    static final String MAIN = "main"
    final Scope retrolambda
    final Scope postProcess

    JavaLibTarget(Project project, String name) {
        super(project, name)

        // Retrolambda
        if (project.plugins.hasPlugin('me.tatarka.retrolambda')) {
            retrolambda = new Scope(project, ["retrolambdaConfig"] as Set)
        } else {
            retrolambda = null
        }

        postProcess = new Scope(project.getRootProject(), [OkBuckGradlePlugin.CONFIGURATION_POST_PROCESS] as Set)
    }

    @Override
    Scope getMain() {
        return new Scope(project,
                ["compile"],
                project.files("src/main/java") as Set,
                project.file("src/main/resources"),
                project.compileJava.options.compilerArgs as List)
    }

    @Override
    Scope getTest() {
        return new Scope(project,
                ["testCompile"],
                project.files("src/test/java") as Set,
                project.file("src/test/resources"),
                project.compileTestJava.options.compilerArgs as List)
    }

    String getSourceCompatibility() {
        return javaVersion(project.sourceCompatibility)
    }

    String getTargetCompatibility() {
        return javaVersion(project.sourceCompatibility)
    }

    String getRetroLambdaJar() {
        retrolambda.externalDeps[0]
    }

    String getBootClasspath() {
        return project.compileJava.options.bootClasspath
    }

    List<String> getPostProcessClassesCommands() {
        return (List<String>) getProp(okbuck.postProcessClassesCommands, [])
    }
}
