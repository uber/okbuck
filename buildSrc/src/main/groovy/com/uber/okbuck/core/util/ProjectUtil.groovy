package com.uber.okbuck.core.util

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.dependency.DependencyCache
import com.uber.okbuck.core.model.base.ProjectType
import com.uber.okbuck.core.model.base.Target
import com.uber.okbuck.core.model.base.TargetCache
import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin

final class ProjectUtil {

    private ProjectUtil() {
        // no instance
    }

    static ProjectType getType(Project project) {
        if (project.plugins.hasPlugin(AppPlugin)) {
            return ProjectType.ANDROID_APP
        } else if (project.plugins.hasPlugin(LibraryPlugin)) {
            return ProjectType.ANDROID_LIB
        } else if (project.plugins.hasPlugin(GroovyPlugin)) {
            return ProjectType.GROOVY_LIB
        } else if (project.plugins.hasPlugin(ApplicationPlugin)) {
            return ProjectType.JAVA_APP
        } else if (project.plugins.hasPlugin(JavaPlugin)) {
            return ProjectType.JAVA_LIB
        } else {
            return ProjectType.UNKNOWN
        }
    }

    static DependencyCache getDependencyCache(Project project) {
        return getPlugin(project).depCache
    }

    static Map<String, Target> getTargets(Project project) {
        return getTargetCache(project).getTargets(project)
    }

    static Target getTargetForOutput(Project targetProject, File output) {
        return getTargetCache(targetProject).getTargetForOutput(targetProject, output)
    }

    static OkBuckGradlePlugin getPlugin(Project project) {
        return project.rootProject.plugins.getPlugin(OkBuckGradlePlugin)
    }

    private static TargetCache getTargetCache(Project project) {
        return getPlugin(project).targetCache
    }
}
