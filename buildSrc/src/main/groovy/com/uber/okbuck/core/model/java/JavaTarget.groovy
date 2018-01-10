package com.uber.okbuck.core.model.java

import com.android.builder.model.LintOptions
import com.google.common.base.Preconditions
import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.model.base.Scope
import com.uber.okbuck.core.model.jvm.JvmTarget
import com.uber.okbuck.core.util.LintUtil
import org.gradle.api.JavaVersion
import org.gradle.api.Project

abstract class JavaTarget extends JvmTarget {

    protected static final String UNIT_TEST_PREFIX = "test"

    public static final Set<String> JAVA_DEPS_CONFIGS = ["runtimeClasspath"]
    private static final Set<String> JAVA_PROVIDED_DEPS_CONFIGS = ["compileClasspath"]
    private static final Set<String> JAVA_APT_CONFIGS = ["apt", "annotationProcessorClasspath"]

    JavaTarget(Project project, String name) {
        super(project, name)
    }

    protected static Set<String> getDepsConfigs() {
        return JAVA_DEPS_CONFIGS
    }

    protected static Set<String> getProvidedDepsConfigs() {
        return JAVA_PROVIDED_DEPS_CONFIGS
    }

    protected static Set<String> getAptConfigs() {
        return JAVA_APT_CONFIGS
    }

    /**
     * Apt Scope
     */
    Scope getApt() {
        return Scope.from(project, aptConfigs)
    }

    /**
     * Test Apt Scope
     */
    Scope getTestApt() {
        return Scope.from(project, expand(aptConfigs, UNIT_TEST_PREFIX))
    }

    /**
     * Provided Scope
     */
    Scope getProvided() {
        return Scope.from(project, providedDepsConfigs)
    }

    /**
     * Test Provided Scope
     */
    Scope getTestProvided() {
        return Scope.from(project, expand(providedDepsConfigs, UNIT_TEST_PREFIX))
    }

    /**
     * Expands configuration names to java configuration conventions
     */
    protected Set<String> expand(Set<String> configNames, String prefix = "") {
        Preconditions.checkArgument(!prefix.isEmpty(), "Empty prefix not allowed for java rules")
        return configNames.collect { String configName ->
            "${prefix}${configName.capitalize()}"
        }
    }

    /**
     * Lint Scope
     */
    Scope getLint() {
        File res = null
        Set<File> sourceDirs = []
        List<String> jvmArguments = []
        return Scope.from(project,
                Collections.singleton(OkBuckGradlePlugin.BUCK_LINT),
                sourceDirs,
                res,
                jvmArguments,
                LintUtil.getLintDepsCache(project))
    }

    LintOptions getLintOptions() {
        return null
    }

    boolean hasLintRegistry() {
        try {
            return project.jar.manifest.attributes.containsKey("Lint-Registry")
        } catch (Exception ignored) {
            return false
        }
    }

    /**
     * List of annotation processor classes.
     */
    Set<String> getAnnotationProcessors() {
        return apt.getAnnotationProcessors()
    }

    /**
     * List of test annotation processor classes.
     */
    Set<String> getTestAnnotationProcessors() {
        return testApt.getAnnotationProcessors()
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
