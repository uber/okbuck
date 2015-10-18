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

package com.github.piasy.okbuck.dependency

import com.github.piasy.okbuck.generator.configs.ThirdPartyDependencyBUCKFile
import com.github.piasy.okbuck.helper.ProjectHelper
import com.github.piasy.okbuck.rules.KeystoreRule
import org.apache.commons.io.IOUtils
import org.gradle.api.Project

public class DependencyProcessor {
    private final Project mRootProject
    private final DependencyAnalyzer mDependencyAnalyzer
    private final File mOkBuckDir
    private final String mKeystoreDir
    private final String mSignConfigName

    public DependencyProcessor(
            Project rootProject, DependencyAnalyzer analyzer, File okBuckDir, String keystoreDir,
            String signConfigName
    ) {
        mRootProject = rootProject
        mDependencyAnalyzer = analyzer
        mOkBuckDir = okBuckDir
        mKeystoreDir = keystoreDir
        mSignConfigName = signConfigName
    }

    /**
     * copy dependencies into .okbuck dir, and generate BUCK file for them
     * */
    public void process() {
        processCompileDependencies()

        processAptDependencies()

        processKeystore()
    }

    private void processKeystore() {
        for (Project project : mRootProject.subprojects) {
            if (ProjectHelper.getSubProjectType(
                    project) == ProjectHelper.ProjectType.AndroidAppProject) {
                File keystoreDir = new File(
                        "${mRootProject.projectDir.absolutePath}/${mKeystoreDir}${ProjectHelper.getPathDiff(mRootProject, project)}")
                KeystoreRule keystoreRule = ProjectHelper.createKeystoreRule(project,
                        mSignConfigName, keystoreDir)

                PrintStream printer = new PrintStream("${keystoreDir.absolutePath}/BUCK")
                keystoreRule.print(printer)
                printer.close()
            }
        }
    }

    private void processAptDependencies() {
        Map<Project, Set<File>> aptDependencies = mDependencyAnalyzer.aptDependencies
        for (Project project : aptDependencies.keySet()) {
            File dir = new File(mOkBuckDir.absolutePath + File.separator +
                    "annotation_processor_deps" +
                    ProjectHelper.getPathDiff(mRootProject, project))
            if (!dir.exists()) {
                dir.mkdirs()
                PrintStream printer = new PrintStream(
                        new File(dir.absolutePath + File.separator + "BUCK"))
                new ThirdPartyDependencyBUCKFile(true).print(printer)
                printer.close()
            }
            for (File dependency : aptDependencies.get(project)) {
                println "copying ${dependency.absolutePath} into ${dir.absolutePath}"
                IOUtils.copy(new FileInputStream(dependency), new FileOutputStream(
                        new File(dir.absolutePath + File.separator + dependency.name)))
            }
        }
    }

    private void processCompileDependencies() {
        Map<Project, Set<Dependency>> finalDependenciesGraph = mDependencyAnalyzer.finalDependenciesGraph
        for (Project project : finalDependenciesGraph.keySet()) {
            for (Dependency dependency : finalDependenciesGraph.get(project)) {
                if (!dependency.isInternalDependency(mRootProject)) {
                    if (!dependency.dstDirExists()) {
                        dependency.dstDir.mkdirs()
                        PrintStream printer = new PrintStream(
                                new File(dependency.dstDir.absolutePath + File.separator + "BUCK"))
                        new ThirdPartyDependencyBUCKFile(false).print(printer)
                        printer.close()
                    }
                    dependency.copyTo(mRootProject)
                }
            }
        }
    }
}