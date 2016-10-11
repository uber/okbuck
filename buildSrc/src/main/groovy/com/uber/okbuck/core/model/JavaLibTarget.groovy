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

    protected final List<String> extraJvmArgs = []

    JavaLibTarget(Project project, String name) {
        super(project, name)

        // Retrolambda
        if (project.plugins.hasPlugin('me.tatarka.retrolambda')) {
            retrolambda = new Scope(project, ["retrolambdaConfig"] as Set)
            extraJvmArgs.addAll(["-bootclasspath", bootClasspath])
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
                project.compileJava.options.compilerArgs + extraJvmArgs as List<String>)
    }

    @Override
    Scope getTest() {
        return new Scope(project,
                ["testCompile"],
                project.files("src/test/java") as Set,
                project.file("src/test/resources"),
                project.compileTestJava.options.compilerArgs + extraJvmArgs as List<String>)
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
        List<String> classpaths = []
        if (initialBootCp) {
            classpaths.add(initialBootCp)
        }
        if (retrolambda) {
            classpaths.add(ProjectUtil.runtimeJar)
        }
        return classpaths.join(":")
    }

    List<String> getPostProcessClassesCommands() {
        return (List<String>) getProp(okbuck.postProcessClassesCommands, [])
    }

    protected String getInitialBootCp() {
        return project.compileJava.options.bootClasspath
    }
}
