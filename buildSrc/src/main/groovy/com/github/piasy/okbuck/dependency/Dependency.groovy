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

import com.github.piasy.okbuck.helper.StringUtil
import org.apache.commons.io.IOUtils
import org.gradle.api.Project

/**
 * Dependency presentation.
 * */
public class Dependency {
    private final File mDepFile
    private final File mDstDir
    private final String mSrcCanonicalName
    private final String mResCanonicalName

    public Dependency(File depFile, File dstDir, String srcCanonicalName, String resCanonicalName) {
        mDepFile = depFile
        mDstDir = dstDir
        mSrcCanonicalName = srcCanonicalName
        mResCanonicalName = resCanonicalName
    }

    public String getDepFileName() {
        return mDepFile.name
    }

    public String getSrcCanonicalName() {
        return mSrcCanonicalName
    }

    public boolean hasResPart() {
        return !StringUtil.isEmpty(mResCanonicalName)
    }

    public String getResCanonicalName() throws IllegalStateException {
        if (StringUtil.isEmpty(mResCanonicalName)) {
            throw new IllegalStateException(
                    "dependency ${mDepFile.absolutePath} doesn't have res part")
        }
        return mResCanonicalName
    }

    public boolean isInternalDependency(Project rootProject) {
        return internalDependencyProject(rootProject, mDepFile) != null
    }

    public boolean dstDirExists() {
        return mDstDir.exists()
    }

    public File getDstDir() {
        return mDstDir
    }

    public void copyTo(Project rootProject) throws IllegalStateException {
        if (internalDependencyProject(rootProject, mDepFile) != null) {
            throw new IllegalStateException(
                    "${mDepFile.absolutePath} is an internal dependency, can't be copied")
        }
        if (!dstDirExists()) {
            mDstDir.mkdirs()
        }
        println "copying ${mDepFile.absolutePath} into ${mDstDir.absolutePath}"
        IOUtils.copy(new FileInputStream(mDepFile), new FileOutputStream(
                new File(mDstDir.absolutePath + File.separator + mDepFile.name)))
    }

    /**
     * if the {@code depFile} is an internal dependency, return the project of this internal
     * dependency, if not, return null.
     * */
    public static Project internalDependencyProject(Project rootProject, File depFile) {
        for (Project project : rootProject.subprojects) {
            if (depFile.absolutePath.startsWith(project.buildDir.absolutePath)) {
                return project
            }
        }

        return null
    }
}