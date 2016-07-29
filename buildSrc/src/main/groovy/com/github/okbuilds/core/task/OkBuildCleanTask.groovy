package com.github.okbuilds.core.task

import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * A task to cleanup generated configuration files
 */
class OkBuildCleanTask extends DefaultTask {

    @Input
    Set<String> remove

    @Input
    Set<String> keep

    @TaskAction
    void clean() {
        new FileNameFinder().getFileNames(project.projectDir.absolutePath, remove.join(' '), keep.join(' ')).each {
            FileUtils.deleteQuietly(project.file(it))
        }
    }
}
