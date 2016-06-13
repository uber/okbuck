package com.github.okbuilds.core.model

import org.gradle.api.Project
import org.gradle.internal.jvm.Jvm
/**
 * A java library target
 */
class JavaLibTarget extends JavaTarget {

    static final String MAIN = "main"
    final Scope retrolambda

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
    }

    @Override
    Scope getMain() {
        return new Scope(project, ["compile"], project.files("src/main/java") as Set)
    }

    @Override
    Scope getTest() {
        return new Scope(project, ["testCompile"], project.files("src/test/java") as Set)
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

    List<String> getJvmArgs() {
        return project.compileJava.options.compilerArgs + extraJvmArgs
    }

    String getBootClasspath() {
        String bootCp = initialBootCp
        if (retrolambda) {
            bootCp += ":${Jvm.current().getRuntimeJar()}"
        }
        return bootCp
    }

    protected String getInitialBootCp() {
        return project.compileJava.options.bootClasspath
    }
}
