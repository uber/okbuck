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
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * task added: okbuck, okbuckDebug, okbuckRelease, and okbuck is the shortcut for okbuckRelease
 * */
class OkBuckGradlePlugin implements Plugin<Project> {

    void apply(Project project) {
        project.extensions.create("okbuck", OkBuckExtension)

        Task okBuckClean = project.task('okbuckClean')
        okBuckClean << {
            if (project.okbuck.overwrite) {
                File keyStoreDir = new File(project.projectDir.absolutePath + File.separator +
                        (String) project.okbuck.keystoreDir)
                keyStoreDir.deleteDir()
                File dotOkBuck = new File(
                        "${project.projectDir.absolutePath}${File.separator}.okbuck")
                dotOkBuck.deleteDir()
                File dotBuckConfig = new File(
                        "${project.projectDir.absolutePath}${File.separator}.buckconfig")
                dotBuckConfig.delete()
                project.subprojects { prj ->
                    File buck = new File("${prj.projectDir.absolutePath}${File.separator}BUCK")
                    buck.delete()
                }
            }
        }

        project.getTasksByName("clean", true).each { task ->
            task.dependsOn(okBuckClean)
        }

        Task okBuck = project.task('okbuck')
        dependsOnBuild(okBuck, project)
        okBuck.dependsOn(okBuckClean)
        okBuck << {
            applyWithBuildVariant(project, "release")
        }

        Task okBuckDebug = project.task('okbuckDebug')
        dependsOnBuild(okBuckDebug, project)
        okBuckDebug.dependsOn(okBuckClean)
        okBuckDebug << {
            applyWithBuildVariant(project, "debug")
        }

        Task okBuckRelease = project.task('okbuckRelease')
        dependsOnBuild(okBuckRelease, project)
        okBuckRelease.dependsOn(okBuckClean)
        okBuckRelease << {
            applyWithBuildVariant(project, "release")
        }
    }

    private static dependsOnBuild(Task task, Project project) {
        project.getTasksByName("bundleRelease", true).each { bundleRelease ->
            task.dependsOn(bundleRelease)
        }
        project.getTasksByName("jar", true).each { jar ->
            task.dependsOn(jar)
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
        printDeps(project, dependencyAnalyzer)

        // step 4: generate BUCK file for each sub project
        File thirdPartyLibsDir = new File(".okbuck")
        if (thirdPartyLibsDir.exists() && !overwrite) {
            throw new IllegalStateException(
                    "third-party libs dir already exist, set overwrite property to true to overwrite existing file.")
        } else {
            new BuckFileGenerator(project, dependencyAnalyzer, thirdPartyLibsDir,
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
            DependencyAnalyzer dependencyAnalyzer
    ) {
        project.subprojects { prj ->
            println "${prj.name}'s deps:"
            println "<<< internal"
            for (Project projectDep : dependencyAnalyzer.allSubProjectsInternalDependencies.get(prj.name)) {
                println "\t${projectDep.name}"
            }
            println "<<< internal excluded"
            for (Project projectDep : dependencyAnalyzer.allSubProjectsInternalDependenciesExcluded.get(prj.name)) {
                println "\t${projectDep.name}"
            }
            println ">>>\n<<< external"
            for (File mavenDep : dependencyAnalyzer.allSubProjectsExternalDependencies.get(prj.name)) {
                println "\t${mavenDep.absolutePath}"
            }
            println ">>>\n<<< external excluded"
            for (File mavenDep : dependencyAnalyzer.allSubProjectsExternalDependenciesExcluded.get(prj.name)) {
                println "\t${mavenDep.absolutePath}"
            }
            println ">>>\n<<< apt"
            for (File mavenDep : dependencyAnalyzer.allSubProjectsAptDependencies.get(prj.name)) {
                println "\t${mavenDep.absolutePath}"
            }
            println ">>>\n<<< annotation processors"
            for (String processor : dependencyAnalyzer.annotationProcessors.get(prj.name)) {
                println "\t${processor}"
            }
            println ">>>"
        }
    }
}
