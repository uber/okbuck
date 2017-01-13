package com.uber.okbuck.core.model.base

import com.android.build.gradle.api.BaseVariant
import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.core.model.android.AndroidLibTarget
import com.uber.okbuck.core.model.groovy.GroovyLibTarget
import com.uber.okbuck.core.model.java.JavaAppTarget
import com.uber.okbuck.core.model.java.JavaLibTarget
import com.uber.okbuck.core.model.jvm.JvmTarget
import com.uber.okbuck.core.util.ProjectUtil
import org.gradle.api.Project

class TargetCache {

    private final Map<Project, Map<String, Target>> store = [:]
    private final Map<File, Target> outputToTarget = [:]

    Map<String, Target> getTargets(Project project) {
        Map<String, Target> projectTargets = store.get(project)
        if (projectTargets == null) {
            ProjectType type = ProjectUtil.getType(project)
            switch (type) {
                case ProjectType.ANDROID_APP:
                    projectTargets = project.android.applicationVariants.collectEntries { BaseVariant variant ->
                        [variant.name, new AndroidAppTarget(project, variant.name)]
                    }
                    break
                case ProjectType.ANDROID_LIB:
                    projectTargets = [:]
                    project.android.libraryVariants.each { BaseVariant variant ->
                        Target target = new AndroidLibTarget(project, variant.name)
                        projectTargets.put(variant.name, target)
                        variant.outputs.each {
                            outputToTarget.put(it.outputFile, target)
                        }
                    }
                    break
                case ProjectType.GROOVY_LIB:
                    projectTargets = ["${JvmTarget.MAIN}": new GroovyLibTarget(project, JvmTarget.MAIN)]
                    break
                case ProjectType.JAVA_APP:
                    projectTargets = ["${JvmTarget.MAIN}": new JavaAppTarget(project, JvmTarget.MAIN)]
                    break
                case ProjectType.JAVA_LIB:
                    projectTargets = ["${JvmTarget.MAIN}": new JavaLibTarget(project, JvmTarget.MAIN)]
                    break
                default:
                    projectTargets = [:]
                    break
            }
            store.put(project, projectTargets)
        }
        return projectTargets
    }

    @SuppressWarnings("GrReassignedInClosureLocalVar")
    Target getTargetForOutput(Project targetProject, File output) {
        Target result
        ProjectType type = ProjectUtil.getType(targetProject)
        switch (type) {
            case ProjectType.ANDROID_LIB:
                result = outputToTarget.get(output)
                break
            case ProjectType.GROOVY_LIB:
            case ProjectType.JAVA_APP:
            case ProjectType.JAVA_LIB:
                result = getTargets(targetProject).values()[0] // Only one target
                break
            default:
                result = null
        }
        return result
    }
}
