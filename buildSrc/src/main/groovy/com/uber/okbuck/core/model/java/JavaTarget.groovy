package com.uber.okbuck.core.model.java

import com.android.builder.model.LintOptions
import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.model.base.Scope
import com.uber.okbuck.core.model.jvm.JvmTarget
import com.uber.okbuck.core.util.LintUtil
import com.uber.okbuck.core.util.ProjectUtil
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.UnknownConfigurationException

abstract class JavaTarget extends JvmTarget {

    protected static final String TEST_PREFIX = "test"

    private static final List<String> JAVA_COMPILE_CONFIGS = ["compile"]
    private static final List<String> JAVA_APT_CONFIGS = ["apt", "compileOnly"]
    public static final List<String> JAVA_PROVIDED_CONFIGS = ["provided", "compileOnly"]

    JavaTarget(Project project, String name) {
        super(project, name)
    }

    protected static List<String> getCompileConfigs() {
        return JAVA_COMPILE_CONFIGS
    }

    protected List<String> getAptConfigs() {
        return JAVA_APT_CONFIGS
    }

    protected List<String> getProvidedConfigs() {
        return JAVA_PROVIDED_CONFIGS
    }

    /**
     * Apt Scope
     */
    Scope getApt() {
        return new Scope(project, expand(aptConfigs))
    }

    /**
     * Test Apt Scope
     */
    Scope getTestApt() {
        return new Scope(project, expand(aptConfigs, TEST_PREFIX))
    }

    /**
     * Provided Scope
     */
    Scope getProvided() {
        return new Scope(project, expand(providedConfigs))
    }

    /**
     * Test Provided Scope
     */
    Scope getTestProvided() {
        return new Scope(project, expand(providedConfigs, TEST_PREFIX))
    }

    Set<String> getDepConfigNames() {
        return compileConfigs + aptConfigs + providedConfigs +
                expand(compileConfigs + aptConfigs + providedConfigs, TEST_PREFIX)
    }

    Set<Configuration> depConfigurations() {
        Set<Configuration> configurations = new HashSet()
        depConfigNames.each { String configName ->
            try {
                Configuration configuration = project.configurations.getByName(configName)
                if (configuration.dependencies)
                    configurations.add(project.configurations.getByName(configName))
            } catch (UnknownConfigurationException ignored) {
            }
        }
        return Scope.useful(configurations)
    }

    /**
     * Expands configuration names to java configuration conventions
     */
    protected Set<String> expand(List<String> configNames, String prefix = "", boolean includeParent = false) {
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
        return new Scope(project, [OkBuckGradlePlugin.BUCK_LINT], sourceDirs, res, jvmArguments,
                LintUtil.getLintDepsCache(project))
    }

    /**
     * Lint libraries Scope
     */
    Scope getLintLibraries() {
        File res = null
        Set<File> sourceDirs = []
        List<String> jvmArguments = []
        return new Scope(project, [OkBuckGradlePlugin.BUCK_LINT_LIBRARY], sourceDirs, res, jvmArguments, ProjectUtil.getDependencyCache(project))
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
