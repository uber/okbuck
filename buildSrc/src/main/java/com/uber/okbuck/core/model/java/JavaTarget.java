package com.uber.okbuck.core.model.java;

import com.android.builder.model.LintOptions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.core.model.jvm.JvmTarget;
import com.uber.okbuck.core.util.LintUtil;

import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.jvm.tasks.Jar;

import java.util.Set;

import javax.annotation.Nullable;

public abstract class JavaTarget extends JvmTarget {

    public JavaTarget(Project project, String name) {
        super(project, name);
    }

    /**
     * Apt Scope
     */
    public Scope getApt() {
        return Scope.from(getProject(), JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME);
    }

    /**
     * Test Apt Scope
     */
    public Scope getTestApt() {
        return Scope.from(getProject(), JavaPlugin.TEST_ANNOTATION_PROCESSOR_CONFIGURATION_NAME);
    }

    /**
     * Provided Scope
     */
    public Scope getProvided() {
        return Scope.from(getProject(), JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME);
    }

    /**
     * Test Provided Scope
     */
    public Scope getTestProvided() {
        return Scope.from(getProject(), JavaPlugin.TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME);
    }

    /**
     * Lint Scope
     */
    public Scope getLint() {
        return Scope.from(getProject(), OkBuckGradlePlugin.BUCK_LINT, ImmutableSet.of(), ImmutableSet.of(),
                ImmutableList.of(), LintUtil.getLintDepsCache(getProject()));
    }

    @Nullable
    public LintOptions getLintOptions() {
        return null;
    }

    public boolean hasLintRegistry() {
        Jar jarTask = (Jar) getProject().getTasks().findByName(JavaPlugin.JAR_TASK_NAME);
        if (jarTask != null) {
            return jarTask.getManifest().getAttributes().containsKey("Lint-Registry")
                    || jarTask.getManifest().getAttributes().containsKey("Lint-Registry-v2");
        }
        return false;
    }

    /**
     * List of annotation processor classes.
     */
    public Set<String> getAnnotationProcessors() {
        return getApt().getAnnotationProcessors();
    }

    /**
     * List of test annotation processor classes.
     */
    public Set<String> getTestAnnotationProcessors() {
        return getTestApt().getAnnotationProcessors();
    }

    protected static String javaVersion(JavaVersion version) {
        return version.getMajorVersion();
    }
}
