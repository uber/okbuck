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

import com.github.piasy.okbuck.configs.ThirdPartyDependencyBUCKFile
import org.gradle.api.Project

public final class DependencyProcessor {
    private final DependencyAnalyzer mDependencyAnalyzer

    public DependencyProcessor(DependencyAnalyzer analyzer) {
        mDependencyAnalyzer = analyzer
    }

    /**
     * copy dependencies into .okbuck dir, and generate BUCK file for them
     * */
    public void process() {
        processCompileDependencies()

        processAptDependencies()
    }

    private void processAptDependencies() {
        for (Project project : mDependencyAnalyzer.aptDependencies.keySet()) {
            for (Dependency dependency : mDependencyAnalyzer.aptDependencies.get(project)) {
                createBuckFileIfNeed(dependency, true)
                copyDependencyIfNeed(dependency)
            }
        }
    }

    private void processCompileDependencies() {
        for (Project project : mDependencyAnalyzer.finalDependencies.keySet()) {
            for (String flavor : mDependencyAnalyzer.finalDependencies.get(project).keySet()) {
                for (Dependency dependency :
                        mDependencyAnalyzer.finalDependencies.get(project).get(flavor)) {
                    createBuckFileIfNeed(dependency, false)
                    copyDependencyIfNeed(dependency)
                }
            }
        }
        for (Project project : mDependencyAnalyzer.fullDependencies.keySet()) {
            for (String flavor : mDependencyAnalyzer.fullDependencies.get(project).keySet()) {
                for (Dependency dependency :
                        mDependencyAnalyzer.fullDependencies.get(project).get(flavor)) {
                    createBuckFileIfNeed(dependency, false)
                    copyDependencyIfNeed(dependency)
                }
            }
        }
    }

    private static void createBuckFileIfNeed(Dependency dependency, boolean includeShortCut) {
        if (dependency.shouldCopy()) {
            if (!dependency.dstDir.exists()) {
                dependency.dstDir.mkdirs()
                PrintStream printer = new PrintStream(
                        new File(dependency.dstDir.absolutePath + File.separator + "BUCK"))
                new ThirdPartyDependencyBUCKFile(includeShortCut).print(printer)
                printer.close()
            }
        }
    }

    private static void copyDependencyIfNeed(Dependency dependency) {
        if (dependency.shouldCopy()) {
            dependency.copyTo()
        }
    }
}