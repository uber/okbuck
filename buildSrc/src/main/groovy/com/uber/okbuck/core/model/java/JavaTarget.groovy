package com.uber.okbuck.core.model.java

import com.android.builder.model.LintOptions
import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.model.base.Scope
import com.uber.okbuck.core.model.jvm.JvmTarget
import com.uber.okbuck.core.util.LintUtil
import org.gradle.api.JavaVersion
import org.gradle.api.Project

abstract class JavaTarget extends JvmTarget {

    protected static final String TEST_PREFIX = "test"

    private static final Set<String> JAVA_COMPILE_CONFIGS = ["compile"]
    private static final Set<String> JAVA_APT_CONFIGS = ["apt"]
    public static final Set<String> JAVA_PROVIDED_CONFIGS = ["compileOnly", "provided"]

    JavaTarget(Project project, String name) {
        super(project, name)
    }

    protected static Set<String> getCompileConfigs() {
        return JAVA_COMPILE_CONFIGS
    }

    protected Set<String> getAptConfigs() {
        return JAVA_APT_CONFIGS
    }

    protected Set<String> getProvidedConfigs() {
        return JAVA_PROVIDED_CONFIGS
    }

    /**
     * Apt Scope
     */
    Scope getApt() {
        return Scope.from(project, expand(aptConfigs))
    }

    /**
     * Test Apt Scope
     */
    Scope getTestApt() {
        return Scope.from(project, expand(aptConfigs, TEST_PREFIX))
    }

    /**
     * Provided Scope
     */
    Scope getProvided() {
        return Scope.from(project, expand(providedConfigs))
    }

    /**
     * Test Provided Scope
     */
    Scope getTestProvided() {
        return Scope.from(project, expand(providedConfigs, TEST_PREFIX))
    }

    /**
     * Expands configuration names to java configuration conventions
     */
    protected Set<String> expand(Set<String> configNames, String prefix = "", boolean includeParent = false) {
        Set<String> expanded
        if (prefix) {
            expanded = configNames.collect {
                "${prefix}${it.capitalize()}"
            }
        } else {
            expanded = configNames
        }

        if (prefix && includeParent) {
            expanded += expand(configNames)
        }

        return expanded
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
