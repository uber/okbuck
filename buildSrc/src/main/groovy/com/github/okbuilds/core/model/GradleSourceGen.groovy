package com.github.okbuilds.core.model

import org.gradle.api.Task

class GradleSourcegen {

    Task task
    Set<String> inputs
    File outputDir

    GradleSourcegen(task, inputs, outputDir) {
        this.task = task
        this.inputs = inputs
        this.outputDir = outputDir
    }
}
