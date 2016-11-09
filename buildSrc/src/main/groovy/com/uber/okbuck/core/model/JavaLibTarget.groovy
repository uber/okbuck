package com.uber.okbuck.core.model

import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.util.ProjectUtil
import com.uber.okbuck.core.util.RetrolambdaUtil
import com.uber.okbuck.extension.ExperimentalExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project

/**
 * A java library target
 */
class JavaLibTarget extends JavaTarget {

    static final String MAIN = "main"

    JavaLibTarget(Project project, String name) {
        super(project, name)
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
        return javaVersion(project.targetCompatibility)
    }

    boolean getRetrolambda() {
        ExperimentalExtension experimental = project.rootProject.okbuck.experimental
        return experimental.retrolambda &&
                project.plugins.hasPlugin('me.tatarka.retrolambda') &&
                JavaVersion.toVersion(sourceCompatibility) > JavaVersion.VERSION_1_7
    }

    List<Map<String, String>> getTransforms() {
        return (List<Map<String, String>>) getProp(okbuck.transform.transforms, [])
    }

    String getTransformRunnerClass() {
        return okbuck.transform.main
    }
}
