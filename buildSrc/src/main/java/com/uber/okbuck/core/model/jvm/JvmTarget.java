package com.uber.okbuck.core.model.jvm;

import com.android.builder.model.LintOptions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.core.model.base.Target;
import com.uber.okbuck.core.util.LintUtil;

import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.ApplicationPluginConvention;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.tasks.Jar;

import java.io.File;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

public class JvmTarget extends Target {

    public static final String MAIN = "main";

    private final String aptConfigurationName;
    private final String testAptConfigurationName;
    private final SourceSetContainer sourceSets;

    public JvmTarget(Project project, String name) {
        this(project,
                name,
                JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME,
                JavaPlugin.TEST_ANNOTATION_PROCESSOR_CONFIGURATION_NAME);
    }

    public JvmTarget(Project project, String name, String aptConfigurationName, String testAptConfigurationName) {
        super(project, name);
        this.aptConfigurationName = aptConfigurationName;
        this.testAptConfigurationName = testAptConfigurationName;
        sourceSets = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
    }

    /**
     * The test options
     *
     * @return The test options
     */
    public TestOptions getTestOptions() {
        Test testTask = (Test) getProject().getTasks().getByName(JavaPlugin.TEST_TASK_NAME);
        Map<String, Object> env = testTask.getEnvironment();
        env.keySet().removeAll(System.getenv().keySet());
        return new TestOptions(testTask.getAllJvmArgs(), testTask.getEnvironment());
    }

    /**
     * Apt Scope
     */
    public Scope getApt() {
        return Scope.from(getProject(), aptConfigurationName);
    }

    /**
     * Test Apt Scope
     */
    public Scope getTestApt() {
        return Scope.from(getProject(), testAptConfigurationName);
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

    public Scope getMain() {
        JavaCompile compileJavaTask = (JavaCompile) getProject().getTasks()
                .getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME);
        return Scope.from(getProject(),
                JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME,
                getMainSrcDirs(),
                getMainJavaResourceDirs(),
                compileJavaTask.getOptions().getCompilerArgs());
    }

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

    private static String javaVersion(JavaVersion version) {
        return version.getMajorVersion();
    }
}
