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

import com.github.piasy.okbuck.dependency.DependencyAnalyzer
import com.github.piasy.okbuck.dependency.DependencyExtractor
import com.github.piasy.okbuck.dependency.DependencyProcessor
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
                File okbuckDir = new File(project.projectDir.absolutePath +
                        File.separator + ".okbuck")
                okbuckDir.deleteDir()
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
            applyWithoutBuildVariant(project)
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

    private static applyWithoutBuildVariant(Project project) {
        boolean overwrite = project.okbuck.overwrite
        if (overwrite) {
            println "==========>> overwrite mode is toggle on <<=========="
        }

        DependencyAnalyzer analyzer = new DependencyAnalyzer(project,
                new File(project.projectDir.absolutePath + File.separator + ".okbuck"),
                (boolean) project.okbuck.checkDepConflict,
                new DependencyExtractor(project))

        analyzer.analyse()
        new DependencyProcessor(analyzer).process()

        /*// step 1: create .buckconfig
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

        // step 2: generate script files
        File scriptsDir = new File(project.projectDir.absolutePath + File.separator + "okbuck-scripts")
        if (!scriptsDir.exists()) {
            scriptsDir.mkdirs()
        }
        File manifestPyFile = new File(scriptsDir.absolutePath + File.separator + "manifest.py")
        PrintStream printer = new PrintStream(manifestPyFile)
        new GenManifestPyFile().print(printer)
        printer.close()
        File scriptBUCKFile = new File(scriptsDir.absolutePath + File.separator + "BUCK")
        printer = new PrintStream(scriptBUCKFile)
        new ScriptBUCKFile().print(printer)
        printer.close()

        // step 3: analyse dependencies
        File okBuckDir = new File(project.projectDir.absolutePath + File.separator + ".okbuck")
        if (okBuckDir.exists() && !overwrite) {
            throw new IllegalStateException(
                    ".okbuck dir already exist, set overwrite property to true to overwrite existing file.")
        } else {
            DependencyAnalyzer dependencyAnalyzer = new DependencyAnalyzer(project, okBuckDir,
                    (boolean) project.okbuck.checkDepConflict)
            printDeps(project, dependencyAnalyzer)

            // step 4: generate BUCK file for each sub project
            Map<Project, BUCKFile> buckFiles = new XBuckFileGenerator(project, dependencyAnalyzer,
                    okBuckDir, (Map<String, String>) project.okbuck.resPackages,
                    (String) project.okbuck.keystoreDir, (String) project.okbuck.signConfigName,
                    (int) project.okbuck.linearAllocHardLimit,
                    (List<String>) project.okbuck.primaryDexPatterns,
                    (boolean) project.okbuck.exopackage, (String) project.okbuck.appClassSource,
                    (List<String>) project.okbuck.appLibDependencies).
                    generate()
            for (Project subProject : buckFiles.keySet()) {
                File buckFile = new File(
                        subProject.projectDir.absolutePath + File.separator + "BUCK")
                printer = new PrintStream(buckFile)
                buckFiles.get(subProject).print(printer)
                printer.close()
            }
        }*/
    }
}
