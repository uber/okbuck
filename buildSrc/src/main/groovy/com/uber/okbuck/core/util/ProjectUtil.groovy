package com.uber.okbuck.core.util

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.core.model.android.AndroidLibTarget
import com.uber.okbuck.core.model.base.ProjectType
import com.uber.okbuck.core.model.base.Target
import com.uber.okbuck.core.model.groovy.GroovyLibTarget
import com.uber.okbuck.core.model.java.JavaAppTarget
import com.uber.okbuck.core.model.java.JavaLibTarget
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
}
