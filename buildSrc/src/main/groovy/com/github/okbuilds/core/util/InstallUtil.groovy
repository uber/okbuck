package com.github.okbuilds.core.util

import com.github.okbuilds.core.system.BuildSystem
import org.gradle.api.Project

/**
 * Utility class to install a {@link BuildSystem} tool.
 */
class InstallUtil {

    private InstallUtil() {}

    static void install(Project project, BuildSystem build, String gitUrl, String sha, File cacheDir) {
        cacheDir.mkdirs()
        File repoDir = new File(cacheDir, build.name)

        if (!repoDir.exists()) {
            repoDir.mkdirs()
            GitUtil.clone(build.gitUrl, repoDir)
        }

        def remoteUrl = build.gitUrl
        def remoteName = "origin"
        if (gitUrl != null && !gitUrl.empty) {
            remoteUrl = gitUrl
            remoteName = GitUtil.remoteName(remoteUrl)
        }

        GitUtil.addRemote(repoDir, remoteUrl)
        GitUtil.fetchAll(repoDir)
        GitUtil.cleanReset(repoDir)
        GitUtil.checkout(repoDir, sha, remoteName)

        build.installer.install(project, repoDir)
    }
}
