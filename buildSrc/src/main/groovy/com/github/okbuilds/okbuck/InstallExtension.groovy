package com.github.okbuilds.okbuck

import org.gradle.api.Project

class InstallExtension {

    String dir
    String gitUrl = ''
    String sha = "master"

    InstallExtension(Project project) {
        dir = new File(project.gradle.gradleUserHomeDir, "caches/okbuilds").absolutePath
    }
}
