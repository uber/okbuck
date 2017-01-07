package com.uber.okbuck.core.model.java

import com.android.builder.model.LintOptions
import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.model.base.Scope
import com.uber.okbuck.core.model.jvm.JvmTarget
import com.uber.okbuck.core.util.LintUtil
import org.gradle.api.JavaVersion
import org.gradle.api.Project

abstract class JavaTarget extends JvmTarget {

    static final Set<String> APT_CONFIGS = ["apt", "annotationProcessor"]
    static final Set<String> PROVIDED_CONFIGS = ["provided", "compileOnly"]


    JavaTarget(Project project, String name) {
        super(project, name)
    }

    /**
     * Apt Scope
     */
    Scope getApt() {
        return new Scope(project, APT_CONFIGS)
    }

    /**
     * Provided Scope
     */
    Scope getProvided() {
        return new Scope(project, PROVIDED_CONFIGS)
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
