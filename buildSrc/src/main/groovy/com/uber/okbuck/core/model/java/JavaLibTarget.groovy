package com.uber.okbuck.core.model.java

import com.uber.okbuck.core.model.base.Scope
import com.uber.okbuck.core.util.RetrolambdaUtil
import com.uber.okbuck.extension.ExperimentalExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.UnknownConfigurationException

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

    Set<String> getDepConfigNames() {
        return APT_CONFIGS + ["compile", "testCompile"]
    }

    Set<Configuration> depConfigurations() {
        Set<Configuration> configurations = new HashSet()
        depConfigNames.each { String configName ->
            try {
                configurations.add(project.configurations.getByName(configName))
            } catch(UnknownConfigurationException ignored) {}
        }
        return configurations
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

    List<String> getPostprocessClassesCommands() {
        List<String> cmds = []
        if (retrolambda) {
            cmds += RetrolambdaUtil.retrolambdaCmd
        }
        return cmds
    }
}
