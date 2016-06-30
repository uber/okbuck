package com.github.okbuilds.core.util

import com.github.okbuilds.core.system.BuildSystem
import org.gradle.api.Project

/**
 * Utility class to install a {@link BuildSystem} tool.
 */
class InstallUtil {

    private InstallUtil() {}

    static void install(Project project, BuildSystem build, String gitUrl, String ref, File cacheDir) {
        cacheDir.mkdirs()
        File repoDir = new File(cacheDir, build.name)

        if (!repoDir.exists()) {
            repoDir.mkdirs()
            GitUtil.clone(build.gitUrl, repoDir)
        }

        def remoteUrl = build.gitUrl
        if (gitUrl != null && !gitUrl.empty) {
            remoteUrl = gitUrl
        }

        GitUtil.addRemote(repoDir, remoteUrl)
        GitUtil.fetchAll(repoDir)
        GitUtil.checkout(repoDir, ref)

        build.installer.install(project, repoDir)
    }
}
