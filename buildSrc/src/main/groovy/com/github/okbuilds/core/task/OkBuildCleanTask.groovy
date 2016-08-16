package com.github.okbuilds.core.task

import com.github.okbuilds.okbuck.OkBuckExtension
import com.github.okbuilds.okbuck.OkBuckGradlePlugin
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * A task to cleanup generated configuration files
 */
class OkBuildCleanTask extends DefaultTask {

    @TaskAction
    void clean() {
        OkBuckExtension okbuck = project.extensions.getByName(OkBuckGradlePlugin.OKBUCK) as OkBuckExtension

        project.fileTree(dir: project.projectDir.absolutePath, includes: okbuck.remove, excludes: okbuck.keep).each { File f ->
            FileUtils.deleteQuietly(f)
        }
    }
}
