package com.uber.okbuck.extension

import org.gradle.api.Project

class GradleGenExtension {

    /**
     * Location of the gradle binary or wrapper
     */
    File gradle

    /**
     * Extra options to be used when invoking the gradle gen task
     */
    List<String> options = []

    GradleGenExtension(Project project) {
        gradle = project.file("gradlew")
    }
}
