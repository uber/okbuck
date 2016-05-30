package com.github.okbuilds.okbuck

import org.apache.http.annotation.Experimental
import org.gradle.api.Project

@Experimental
class InstallExtension {

    String dir
    String gitUrl = ''
    String sha = "master"

    InstallExtension(Project project) {
        dir = new File(project.gradle.gradleUserHomeDir, "caches/okbuilds").absolutePath
    }
}
