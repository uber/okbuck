package com.github.okbuilds.core.model

import com.github.okbuilds.core.dependency.ExternalDependency
import org.gradle.api.JavaVersion
import org.gradle.api.Project

/**
 * A java library target
 */
class JavaLibTarget extends Target {

    private static final String RETRO_LAMBDA_CONFIG = "retrolambdaConfig"
    static final String MAIN = "main"
    final boolean retrolambda

    private final Set<ExternalDependency> retroLambdaDeps = [] as Set
    protected final List<String> extraJvmArgs = []

    JavaLibTarget(Project project, String name) {
        super(project, name)

        // Retrolambda
        retrolambda = project.plugins.hasPlugin('me.tatarka.retrolambda')
        if (retrolambda) {
            extractConfigurations([RETRO_LAMBDA_CONFIG] as Set, retroLambdaDeps, [] as Set)
            extraJvmArgs.addAll(["-bootclasspath", bootClasspath])
        }
    }

    @Override
    protected Set<File> sourceDirs() {
        return project.files("src/main/java") as Set
    }

    protected Set<File> testSourceDirs() {
        return project.files("src/test/java") as Set
    }

    @Override
    protected Set<String> compileConfigurations() {
        return ["compile"]
    }

    protected Set<String> testCompileConfigurations() {
        return ["testCompile"]
    }

    String getSourceCompatibility() {
        return javaVersion(project.sourceCompatibility)
    }

    String getTargetCompatibility() {
        return javaVersion(project.sourceCompatibility)
    }

    String getRetroLambdaJar() {
        dependencyCache.get(retroLambdaDeps[0])
    }

    List<String> getJvmArgs() {
        return project.compileJava.options.compilerArgs + extraJvmArgs
    }

    String getBootClasspath() {
        String bootCp = initialBootCp
        if (retrolambda) {
            bootCp += ":${rtJar}"
        }
        return bootCp
    }

    protected String getInitialBootCp() {
        return project.compileJava.options.bootClasspath
    }

    protected static String getRtJar() {
        return "${System.properties.'java.home'}/lib/rt.jar"
    }

    protected static String javaVersion(JavaVersion version) {
        switch (version) {
            case JavaVersion.VERSION_1_6:
                return '6'
            case JavaVersion.VERSION_1_7:
                return '7'
            case JavaVersion.VERSION_1_8:
                return '8'
            case JavaVersion.VERSION_1_9:
                return '9'
            default:
                return '7'
        }
    }
}
