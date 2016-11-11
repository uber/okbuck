package com.uber.okbuck.extension

import org.gradle.api.Project

class GradleGenExtension {

    /**
     * The location of the gradle executable. Can be a gradle wrapper. 'gradlew' by default.
     */
    File gradle

    /**
     * Whether to symlink the output of the gradle task into buck's output directory. {@code true} by default. Set to
     * {@code false} to copy the output instead.
     */
    boolean symlinkOutputs = true

    /**
     * Extra gradle options to be used when invoking the gradle gen task.
     */
    List<String> options = []

    GradleGenExtension(Project project) {
        gradle = project.file("gradlew")

    }
}
