package com.uber.okbuck.core.util

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.BaseVariantOutput
import com.uber.okbuck.core.model.AndroidAppTarget
import com.uber.okbuck.core.model.AndroidLibTarget
import com.uber.okbuck.core.model.JavaAppTarget
import com.uber.okbuck.core.model.JavaLibTarget
import com.uber.okbuck.core.model.Target
import com.uber.okbuck.OkBuckExtension
import org.apache.commons.io.FilenameUtils
import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.JavaPlugin

final class ProjectUtil {

    private ProjectUtil() {
        // no instance
    }

    static com.uber.okbuck.core.model.ProjectType getType(Project project) {
        if (project.plugins.hasPlugin(AppPlugin)) {
            return com.uber.okbuck.core.model.ProjectType.ANDROID_APP
        } else if (project.plugins.hasPlugin(LibraryPlugin)) {
            return com.uber.okbuck.core.model.ProjectType.ANDROID_LIB
        } else if (project.plugins.hasPlugin(ApplicationPlugin.class)) {
            return com.uber.okbuck.core.model.ProjectType.JAVA_APP
        } else if (project.plugins.hasPlugin(JavaPlugin)) {
            return com.uber.okbuck.core.model.ProjectType.JAVA_LIB
        } else {
            return com.uber.okbuck.core.model.ProjectType.UNKNOWN
        }
    }

    static Map<String, Target> getTargets(Project project) {
        com.uber.okbuck.core.model.ProjectType type = getType(project)
        switch (type) {
            case com.uber.okbuck.core.model.ProjectType.ANDROID_APP:
                project.android.applicationVariants.collectEntries { BaseVariant variant ->
                    [variant.name, new AndroidAppTarget(project, variant.name)]
                }
                break
            case com.uber.okbuck.core.model.ProjectType.ANDROID_LIB:
                project.android.libraryVariants.collectEntries { BaseVariant variant ->
                    [variant.name, new AndroidLibTarget(project, variant.name)]
                }
                break
            case com.uber.okbuck.core.model.ProjectType.JAVA_APP:
                def targets = new HashMap<String, Target>()
                targets.put(JavaAppTarget.MAIN, new JavaAppTarget(project, JavaAppTarget.MAIN))
                return targets
                break
            case com.uber.okbuck.core.model.ProjectType.JAVA_LIB:
                def targets = new HashMap<String, Target>()
                targets.put(JavaLibTarget.MAIN, new JavaLibTarget(project, JavaLibTarget.MAIN))
                return targets
                break
            default:
                [:]
                break
        }
    }

    static Target getTargetForOutput(Project rootProject, File output) {
        Target result = null
        OkBuckExtension okbuck = rootProject.okbuck
        Project project = okbuck.buckProjects.find { Project project ->
            FilenameUtils.directoryContains(project.buildDir.absolutePath, output.absolutePath)
        }

        if (project != null) {
            com.uber.okbuck.core.model.ProjectType type = getType(project)
            switch (type) {
                case com.uber.okbuck.core.model.ProjectType.ANDROID_LIB:
                    def baseVariants = project.android.libraryVariants
                    baseVariants.all { BaseVariant baseVariant ->
                        def variant = baseVariant.outputs.find { BaseVariantOutput out ->
                            (out.outputFile == output)
                        }
                        if (variant != null) {
                            result = new AndroidLibTarget(project, variant.name)
                        }
                    }
                    break
                case com.uber.okbuck.core.model.ProjectType.JAVA_LIB:
                    result = new JavaLibTarget(project, JavaLibTarget.MAIN)
                    break
                default:
                    result = null
            }
        }
        return result
    }

    static File getRuntimeJar() {
        try {
            final File javaBase = new File(System.getProperty("java.home")).getCanonicalFile();
            File runtimeJar = new File(javaBase, "lib/rt.jar");
            if (runtimeJar.exists()) {
                return runtimeJar;
            }
            runtimeJar = new File(javaBase, "jre/lib/rt.jar");
            return runtimeJar.exists() ? runtimeJar : null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
