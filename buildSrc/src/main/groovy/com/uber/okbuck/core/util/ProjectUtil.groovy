package com.uber.okbuck.core.util

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.core.model.android.AndroidLibTarget
import com.uber.okbuck.core.model.groovy.GroovyLibTarget
import com.uber.okbuck.core.model.java.JavaAppTarget
import com.uber.okbuck.core.model.java.JavaLibTarget
import com.uber.okbuck.core.model.base.ProjectType
import com.uber.okbuck.core.model.base.Target
import com.uber.okbuck.core.model.jvm.JvmTarget
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

    static Map<String, Target> getTargets(Project project) {
        ProjectType type = getType(project)
        switch (type) {
            case ProjectType.ANDROID_APP:
                project.android.applicationVariants.collectEntries { BaseVariant variant ->
                    [variant.name, new AndroidAppTarget(project, variant.name)]
                }
                break
            case ProjectType.ANDROID_LIB:
                project.android.libraryVariants.collectEntries { BaseVariant variant ->
                    [variant.name, new AndroidLibTarget(project, variant.name)]
                }
                break
            case ProjectType.GROOVY_LIB:
                def targets = new HashMap<String, Target>()
                targets.put(JvmTarget.MAIN, new GroovyLibTarget(project, JvmTarget.MAIN))
                return targets
                break
            case ProjectType.JAVA_APP:
                def targets = new HashMap<String, Target>()
                targets.put(JvmTarget.MAIN, new JavaAppTarget(project, JvmTarget.MAIN))
                return targets
                break
            case ProjectType.JAVA_LIB:
                def targets = new HashMap<String, Target>()
                targets.put(JvmTarget.MAIN, new JavaLibTarget(project, JvmTarget.MAIN))
                return targets
                break
            default:
                [:]
                break
        }
    }
}
