package com.uber.okbuck.core.model

import org.gradle.api.Task

class GradleSourceGen {

    Task task
    Set<String> inputs
    File outputDir

    GradleSourceGen(task, inputs, outputDir) {
        this.task = task
        this.inputs = inputs
        this.outputDir = outputDir
    }
}
