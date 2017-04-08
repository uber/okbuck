package com.uber.okbuck.core.model.java

import com.uber.okbuck.core.model.base.Scope
import com.uber.okbuck.core.util.RetrolambdaUtil
import org.gradle.api.JavaVersion
import org.gradle.api.Project

/**
 * A java library target
 */
class JavaLibTarget extends JavaTarget {

    JavaLibTarget(Project project, String name) {
        super(project, name)
    }

    @Override
    Scope getMain() {
        return new Scope(project,
                compileConfigs,
                project.sourceSets.main.java.srcDirs as Set,
                project.file("src/main/resources"),
                project.compileJava.options.compilerArgs as List)
    }

    @Override
    Scope getTest() {
        return new Scope(project,
                expand(compileConfigs, TEST_PREFIX, true),
                project.sourceSets.test.java.srcDirs as Set,
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
        return project.plugins.hasPlugin("me.tatarka.retrolambda") &&
                JavaVersion.toVersion(sourceCompatibility) > JavaVersion.VERSION_1_7
    }

    List<String> getPostprocessClassesCommands() {
        List<String> cmds = []
        if (retrolambda) {
            cmds += RetrolambdaUtil.getRetrolambdaCmd(project)
        }
        return cmds
    }
}
