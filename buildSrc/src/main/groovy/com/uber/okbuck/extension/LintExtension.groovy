package com.uber.okbuck.extension

import com.uber.okbuck.core.annotation.Experimental
import com.uber.okbuck.core.util.LintUtil
import org.gradle.api.Project

@Experimental
class LintExtension {

    /**
     * Lint jar version
     */
    String version

    /**
     * Set to {@code true} to disable generation of lint rules
     */
    boolean disabled = false

    /**
     * JVM arguments when invoking lint
     */
    String jvmArgs = "-Xmx1024m"

    LintExtension(Project project) {
        version = LintUtil.getDefaultLintVersion(project)
    }
}
