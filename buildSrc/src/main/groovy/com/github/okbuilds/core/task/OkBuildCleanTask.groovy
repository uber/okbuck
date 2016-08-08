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
        project.fileTree(dir: project.projectDir.absolutePath, includes: remove, excludes: keep).each { File f ->
            FileUtils.deleteQuietly(f)
        }
    }
}
