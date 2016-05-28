package com.github.okbuilds.core.util

import com.github.okbuilds.core.build.Build
import org.gradle.api.Project

/**
 * Utility class to install a {@link Build} tool.
 */
class InstallUtil {

    private InstallUtil() {}

    static void install(Project project, Build build, String gitUrl, String sha, File cacheDir) {
        cacheDir.mkdirs()
        File repoDir = new File(cacheDir, build.name)

        if (!repoDir.exists()) {
            repoDir.mkdirs()
            GitUtil.clone(build.gitUrl, repoDir)
        }

        if (gitUrl != null && !gitUrl.empty) {
            GitUtil.addRemoteIfNeeded(repoDir, gitUrl)
        }

        GitUtil.fetchAll(repoDir)
        GitUtil.cleanReset(repoDir)
        GitUtil.checkout(repoDir, sha)

        build.installer.install(project, repoDir)
    }
}
