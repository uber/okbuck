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

import com.github.piasy.okbuck.dependency.Dependency
import com.github.piasy.okbuck.dependency.DependencyAnalyzer
import com.github.piasy.okbuck.generator.XBuckFileGenerator
import com.github.piasy.okbuck.generator.XDotBuckConfigGenerator
import com.github.piasy.okbuck.generator.configs.BUCKFile
import com.github.piasy.okbuck.helper.ProjectHelper
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
        hashSetAddTraversalTest()
        boolean overwrite = project.okbuck.overwrite
        if (overwrite) {
            println "==========>> overwrite mode is toggle on <<=========="
        }
        printAllSubProjects(project)


        // step 1: create .buckconfig
        File dotBuckConfig = new File(
                project.projectDir.absolutePath + File.separator + ".buckconfig")
        if (dotBuckConfig.exists() && !overwrite) {
            throw new IllegalStateException(
                    ".buckconfig already exist, set overwrite property to true to overwrite existing file.")
        } else {
            PrintStream printer = new PrintStream(dotBuckConfig)
            new XDotBuckConfigGenerator(project, (String) project.okbuck.target).generate().
                    print(printer)
            printer.close()
        }

        // step 2: analyse dependencies
        File okBuckDir = new File(project.projectDir.absolutePath + File.separator + ".okbuck")
        if (okBuckDir.exists() && !overwrite) {
            throw new IllegalStateException(
                    ".okbuck dir already exist, set overwrite property to true to overwrite existing file.")
        } else {
            DependencyAnalyzer dependencyAnalyzer = new DependencyAnalyzer(project, variant,
                    okBuckDir)
            printDeps(project, dependencyAnalyzer)

            // step 3: generate BUCK file for each sub project
            Map<Project, BUCKFile> buckFiles = new XBuckFileGenerator(project, dependencyAnalyzer,
                    okBuckDir, (Map<String, String>) project.okbuck.resPackages,
                    (String) project.okbuck.keystoreDir, (String) project.okbuck.signConfigName,
                    variant).generate()
            for (Project subProject : buckFiles.keySet()) {
                File buckFile = new File(
                        project.projectDir.absolutePath + ProjectHelper.getPathDiff(project,
                                subProject) + File.separator + "BUCK")
                PrintStream printer = new PrintStream(buckFile)
                buckFiles.get(subProject).print(printer)
                printer.close()
            }
        }
    }

    private static printAllSubProjects(Project project) {
        project.subprojects { prj ->
            println "Sub project '${prj.name}': ${prj.projectDir.absolutePath}"
        }
    }

    private static printDeps(
            Project project,
            DependencyAnalyzer dependencyAnalyzer
    ) {
        project.subprojects { prj ->
            println "${prj.name}'s deps:"
            println "<<< final"
            for (Dependency dependency : dependencyAnalyzer.finalDependenciesGraph.get(prj)) {
                if (dependency.hasResPart()) {
                    println "${dependency.depFileName}: ${dependency.resCanonicalName}, ${dependency.srcCanonicalName}"
                } else {
                    println "${dependency.depFileName}: ${dependency.srcCanonicalName}"
                }
            }
            println ">>>\n<<< apt"
            for (File mavenDep : dependencyAnalyzer.aptDependencies.get(prj)) {
                println "\t${mavenDep.absolutePath}"
            }
            println ">>>\n<<< annotation processors"
            for (String processor : dependencyAnalyzer.annotationProcessors.get(prj)) {
                println "\t${processor}"
            }
            println ">>>"
        }
    }

    private static void hashSetAddTraversalTest() {
        println "<<< hashSetAddTraversalTest"
        Set<String> s1 = new HashSet<>()
        Set<String> s2 = new HashSet<>()
        Set<String> s3 = new HashSet<>()
        s1.add("1")
        s1.add("2")
        s1.add("3")

        s2.add("3")
        s2.add("2")
        s2.add("1")

        s3.add("2")
        s3.add("1")
        s3.add("3")

        String str1 = ""
        for (String str : s1) {
            str1 += str
        }

        String str2 = ""
        for (String str : s2) {
            str2 += str
        }

        String str3 = ""
        for (String str : s3) {
            str3 += str
        }

        println "1, 2, 3 ==> ${str1}"
        println "3, 2, 1 ==> ${str2}"
        println "2, 1, 3 ==> ${str3}"
        println "hashSetAddTraversalTest >>>"
    }
}
