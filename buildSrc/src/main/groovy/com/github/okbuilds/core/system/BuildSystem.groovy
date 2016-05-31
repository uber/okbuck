package com.github.okbuilds.core.system

import com.github.okbuilds.core.util.CmdUtil
import org.gradle.api.Project

enum BuildSystem {

    BUCK('buck', 'git@github.com:facebook/buck.git', new BuckInstaller())

    final String name
    final String gitUrl
    final Installer installer

    private BuildSystem(String name, String gitUrl, Installer installer) {
        this.name = name
        this.gitUrl = gitUrl
        this.installer = installer
    }

    private interface Installer {
        void install(Project project, File buildDir)
    }

    private static final class BuckInstaller implements Installer {

        @Override
        void install(Project project, File buildDir) {
            File buckLink = project.file("buck")
            if (!buckLink.exists()) {
                CmdUtil.run("ln -nsf ${new File(buildDir, 'bin/buck').absolutePath} ${buckLink.absolutePath}")
            }
        }
    }
}
