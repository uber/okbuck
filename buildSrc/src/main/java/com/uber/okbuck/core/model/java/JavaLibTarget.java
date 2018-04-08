package com.uber.okbuck.core.model.java;

import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.core.model.base.Scope;

import org.gradle.api.Project;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.ApplicationPluginConvention;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Set;

/**
 * A java library target
 */
public class JavaLibTarget extends JavaTarget {

    private final SourceSetContainer sourceSets;

    public JavaLibTarget(Project project, String name) {
        this(project,
                name,
                JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME,
                JavaPlugin.TEST_ANNOTATION_PROCESSOR_CONFIGURATION_NAME);
    }

    public JavaLibTarget(Project project, String name, String aptConfigurationName, String testAptConfigurationName) {
        super(project, name, aptConfigurationName, testAptConfigurationName);
        sourceSets = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
    }

    @Override
    public Scope getMain() {
        JavaCompile compileJavaTask = (JavaCompile) getProject().getTasks()
                .getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME);
        return Scope.from(getProject(),
                JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME,
                getMainSrcDirs(),
                getMainJavaResourceDirs(),
                compileJavaTask.getOptions().getCompilerArgs());
    }

    @Override
    public Scope getTest() {
        JavaCompile testCompileJavaTask = (JavaCompile) getProject().getTasks()
                .getByName(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME);
        return Scope.from(getProject(),
                JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME,
                getTestSrcDirs(),
                getTestJavaResourceDirs(),
                testCompileJavaTask.getOptions().getCompilerArgs());
    }

    public String getSourceCompatibility() {
        return javaVersion(getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceCompatibility());
    }

    public String getTargetCompatibility() {
        return javaVersion(getProject().getConvention().getPlugin(JavaPluginConvention.class).getTargetCompatibility());
    }

    public boolean hasApplication() {
        return getProject().getPlugins().hasPlugin(ApplicationPlugin.class);
    }

    @Nullable
    public String getMainClass() {
        return getProject().getConvention().getPlugin(ApplicationPluginConvention.class).getMainClassName();
    }

    public Set<String> getExcludes() {
        Jar jarTask = (Jar) getProject().getTasks().findByName(JavaPlugin.JAR_TASK_NAME);
        return jarTask != null ? jarTask.getExcludes() : ImmutableSet.of();
    }

    private Set<File> getMainSrcDirs() {
        return sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getAllJava().getSrcDirs();
    }

    private Set<File> getMainJavaResourceDirs() {
        return sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getResources().getSrcDirs();
    }

    private Set<File> getTestSrcDirs() {
        return sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME).getAllJava().getSrcDirs();
    }

    private Set<File> getTestJavaResourceDirs() {
        return sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME).getResources().getSrcDirs();
    }
}
