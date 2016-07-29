package com.github.okbuilds.okbuck

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class OkBuckCleanTest {

    @Test
    void should_clean_correctly() {
        Project project = ProjectBuilder.builder().build()

        File buckConfig = project.file('.buckconfig.local')
        buckConfig.createNewFile()

        File buckSymlink = project.file("buck")
        buckSymlink.createNewFile()

        File okbuckCache = project.file('.okbuck')
        okbuckCache.mkdirs()

        File okbuckFile = new File(okbuckCache, "BUCK")
        okbuckFile.createNewFile()

        File projectDir = project.file("project1")
        projectDir.mkdirs()

        File projectFile = new File(projectDir, "BUCK")
        projectFile.createNewFile()

        project.plugins.apply(OkBuckGradlePlugin)
        project.tasks.getByName(OkBuckGradlePlugin.OKBUCK_CLEAN).execute()

        assert !buckConfig.exists()
        assert !projectFile.exists()

        assert buckSymlink.exists()
        assert okbuckFile.exists()
    }
}
