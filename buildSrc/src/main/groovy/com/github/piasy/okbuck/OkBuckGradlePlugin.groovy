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

package com.github.piasy.okbuck

import com.github.piasy.okbuck.analyzer.DependencyAnalyzer
import com.github.piasy.okbuck.generator.BuckFileGenerator
import com.github.piasy.okbuck.generator.DotBuckConfigGenerator
import com.github.piasy.okbuck.helper.AndroidProjectHelper
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * task added: okbuck, okbuckDebug, okbuckRelease, and okbuck is the shortcut for okbuckRelease
 * */
class OkBuckGradlePlugin implements Plugin<Project> {

    void apply(Project project) {
        project.extensions.create("okbuck", OkBuckExtension)

        project.task('okbuck') << {
            /*project.subprojects { prj ->
                if (AndroidProjectHelper.getSubProjectType(prj) == AndroidProjectHelper.ANDROID_APP_PROJECT) {
                    prj.extensions.getByName("android").metaPropertyValues.each { prop ->
                        try {
                            print prop.name
                            print "\t ${prop.type}"
                            print "\t ${prop.value}"
                            print "\n"
                        } catch (Exception e) {
                            print " ... exception...\n"
                        }
                    }
                }
            }*/
            applyWithBuildVariant(project, "release")
        }

        project.task('okbuckDebug') << {
            applyWithBuildVariant(project, "debug")
        }

        project.task('okbuckRelease') << {
            applyWithBuildVariant(project, "release")
        }
    }

    private static applyWithBuildVariant(Project project, String variant) {
        boolean overwrite = project.okbuck.overwrite
        if (overwrite) {
            println "==========>> overwrite mode is toggle on <<=========="
        }
        printAllSubProjects(project)

        // step 1: create .buckconfig
        new DotBuckConfigGenerator(overwrite, project,
                (String) project.okbuck.target).generate()

        // step 2: analyse dependencies
        DependencyAnalyzer dependencyAnalyzer = new DependencyAnalyzer(project, variant)
        dependencyAnalyzer.analyse()

        // step 3: retrieve analyse result
        Map<String, Set<File>> allSubProjectsExternalDependencies = dependencyAnalyzer.allSubProjectsExternalDependencies
        Map<String, Set<String>> allSubProjectsInternalDependencies = dependencyAnalyzer.allSubProjectsInternalDependencies
        Map<String, Set<File>> allSubProjectsAptDependencies = dependencyAnalyzer.allSubProjectsAptDependencies
        Map<String, Set<String>> allSubProjectsAnnotationProcessors = dependencyAnalyzer.annotationProcessors

        printDeps(project, allSubProjectsExternalDependencies,
                allSubProjectsInternalDependencies, allSubProjectsAptDependencies,
                allSubProjectsAnnotationProcessors)

        // step 4: generate BUCK file for each sub project
        File thirdPartyLibsDir = new File(".okbuck")
        if (thirdPartyLibsDir.exists() && !overwrite) {
            throw new IllegalStateException(
                    "third-party libs dir already exist, set overwrite property to true to overwrite existing file.")
        } else {
            new BuckFileGenerator(project, allSubProjectsExternalDependencies,
                    allSubProjectsInternalDependencies, allSubProjectsAptDependencies,
                    allSubProjectsAnnotationProcessors, thirdPartyLibsDir,
                    (Map<String, String>) project.okbuck.resPackages, overwrite,
                    (String) project.okbuck.keystoreDir,
                    (String) project.okbuck.signConfigName, variant).generate()
        }
    }

    private static printAllSubProjects(Project project) {
        project.subprojects { prj ->
            println "Sub project: ${prj.name}"
        }
    }

    private static printDeps(
            Project project,
            Map<String, Set<File>> allSubProjectsExternalDeps,
            Map<String, Set<String>> allSubProjectsInternalDeps,
            Map<String, Set<File>> allSubProjectsAptDeps,
            Map<String, Set<String>> annotationProcessors
    ) {
        project.subprojects { prj ->
            println "${prj.name}'s deps:"
            println "<<< internal"
            for (String projectDep : allSubProjectsInternalDeps.get(prj.name)) {
                println "\t${projectDep}"
            }
            println ">>>\n<<< external"
            for (File mavenDep : allSubProjectsExternalDeps.get(prj.name)) {
                println "\t${mavenDep.absolutePath}"
            }
            println ">>>\n<<< apt"
            for (File mavenDep : allSubProjectsAptDeps.get(prj.name)) {
                println "\t${mavenDep.absolutePath}"
            }
            println ">>>\n<<< annotation processors"
            for (String processor : annotationProcessors.get(prj.name)) {
                println "\t${processor}"
            }
            println ">>>"
        }
    }
}
