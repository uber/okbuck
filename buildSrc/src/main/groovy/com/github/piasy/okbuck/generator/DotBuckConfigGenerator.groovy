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

package com.github.piasy.okbuck.generator

import com.github.piasy.okbuck.helper.AndroidProjectHelper
import org.gradle.api.Project

/**
 * Created by Piasy{github.com/Piasy} on 15/10/6.
 *
 * used to generate .buckconfig file.
 */
class DotBuckConfigGenerator {
    private final boolean mOverwrite
    private final Project mRootProject
    private final String mTarget

    /**
     * Create generator.
     *
     * @param overwrite overwrite existing buck script or not
     * @param rootProject applied rootProject
     */
    public DotBuckConfigGenerator(boolean overwrite, Project rootProject, String target) {
        mOverwrite = overwrite
        mRootProject = rootProject
        mTarget = target
    }

    /**
     * generate .buckconfig
     *
     * @throws RuntimeException when buck script exist and not overwrite
     */
    public void generate() throws RuntimeException {
        File dotBuckConfig = new File(
                "${mRootProject.rootDir.absolutePath}${File.separator}.buckconfig")
        if (dotBuckConfig.exists() && !mOverwrite) {
            throw new IllegalStateException(
                    ".buckconfig file already exist, set overwrite property to true to overwrite existing file.")
        } else {
            println "generating .buckconfig"

            PrintWriter printWriter = new PrintWriter(new FileOutputStream(dotBuckConfig))
            printWriter.println("[alias]")
            mRootProject.subprojects { project ->
                if (AndroidProjectHelper.getSubProjectType(
                        project) == AndroidProjectHelper.ANDROID_APP_PROJECT) {
                    printWriter.println("\t${project.name} = //${project.name}:bin")
                } //else
            }

            printWriter.println()
            printWriter.println("[android]")
            printWriter.println("\ttarget = ${mTarget}")

            printWriter.println()
            printWriter.println("[project]")
            printWriter.println("\tignore = .git, **/.svn")
            printWriter.close()
        }
    }
}
