package com.github.okbuilds.core.task

import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * A task to cleanup generated configuration files
 */
class OkBuckCleanTask extends DefaultTask {

    @Input
    String dir

    @Input
    List<String> includes

    @Input
    List<String> excludes

    @TaskAction
    void clean() {
        project.fileTree(dir: dir, includes: includes, excludes: excludes).each { File f ->
            FileUtils.deleteQuietly(f)
        }
    }
}
