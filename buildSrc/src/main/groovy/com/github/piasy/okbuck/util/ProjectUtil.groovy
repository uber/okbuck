/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Piasy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.piasy.okbuck.util

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.BaseVariantOutput
import com.github.piasy.okbuck.OkBuckExtension
import com.github.piasy.okbuck.model.AndroidAppTarget
import com.github.piasy.okbuck.model.AndroidLibTarget
import com.github.piasy.okbuck.model.JavaLibTarget
import com.github.piasy.okbuck.model.ProjectType
import com.github.piasy.okbuck.model.Target
import groovy.transform.Memoized
import org.apache.commons.io.FilenameUtils
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

/**
 * helper class for sub projects.
 * */
final class ProjectUtil {

    private ProjectUtil() {
        // no instance
    }

    @Memoized
    static ProjectType getType(Project project) {
        if (project.plugins.hasPlugin(AppPlugin)) {
            return ProjectType.ANDROID_APP
        } else if (project.plugins.hasPlugin(LibraryPlugin)) {
            return ProjectType.ANDROID_LIB
        } else if (project.plugins.hasPlugin(JavaPlugin)) {
            return ProjectType.JAVA_LIB
        } else {
            return ProjectType.UNKNOWN
        }
    }

    @Memoized
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
            case ProjectType.JAVA_LIB:
                ["${JavaLibTarget.MAIN}": new JavaLibTarget(project, JavaLibTarget.MAIN)]
                break
            default:
                [:]
                break
        }
    }

    @Memoized
    static Target getTargetForOutput(Project rootProject, File output) {
        Target result = null
        OkBuckExtension okbuck = rootProject.okbuck
        Project project = okbuck.buckProjects.find { Project project ->
            FilenameUtils.directoryContains(project.buildDir.absolutePath, output.absolutePath)
        }

        if (project != null) {
            ProjectType type = getType(project)
            switch (type) {
                case ProjectType.ANDROID_LIB:
                    def baseVariants = project.android.libraryVariants
                    baseVariants.all { BaseVariant baseVariant ->
                        def variant = baseVariant.outputs.find { BaseVariantOutput out ->
                            out.outputFile.equals(output)
                        }
                        if (variant != null) {
                            result = new AndroidLibTarget(project, variant.name)
                        }
                    }
                    break
                case ProjectType.JAVA_LIB:
                    result = new JavaLibTarget(project, JavaLibTarget.MAIN)
                    break
                default:
                    result = null
            }
        }
        return result
    }
}
