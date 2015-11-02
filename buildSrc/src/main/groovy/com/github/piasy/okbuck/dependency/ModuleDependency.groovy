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

import com.github.piasy.okbuck.helper.ProjectHelper
import com.github.piasy.okbuck.helper.StringUtil
import org.gradle.api.Project

/**
 * module dependency: java/android library module.
 *
 * only stands for one Flavor X Variant combination.
 * */
public final class ModuleDependency extends Dependency {

    private final Project mModule

    private final String mResCanonicalName

    private final List<String> mMultipleResCanonicalNames

    private final boolean mHasFlavor

    private final ProjectHelper.ProjectType mProjectType

    public ModuleDependency(
            File depFile, Project module, String srcCanonicalName, String resCanonicalName,
            List<String> multipleResCanonicalNames
    ) {
        super(srcCanonicalName, depFile)
        mModule = module
        mHasFlavor = ProjectHelper.exportFlavor(mModule)
        mProjectType = ProjectHelper.getSubProjectType(mModule)
        mResCanonicalName = resCanonicalName
        mMultipleResCanonicalNames = multipleResCanonicalNames
    }

    @Override
    public String toString() {
        return mModule.projectDir.absolutePath
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof ModuleDependency &&
                ((ModuleDependency) o).mSrcCanonicalName.equals(mSrcCanonicalName)
    }

    @Override
    public int hashCode() {
        return mSrcCanonicalName.hashCode()
    }

    @Override
    boolean hasResPart() {
        return !StringUtil.isEmpty(mResCanonicalName) && mMultipleResCanonicalNames == null
    }

    @Override
    String resCanonicalName() {
        if (hasResPart()) {
            return mResCanonicalName
        } else {
            throw new IllegalStateException(
                    "ModuleDependency ${mModule.projectDir.absolutePath} has no res part")
        }
    }

    @Override
    boolean hasMultipleResPart() {
        return (mProjectType == ProjectHelper.ProjectType.AndroidLibProject ||
                mProjectType ==
                ProjectHelper.ProjectType.AndroidAppProject) && mMultipleResCanonicalNames !=
                null &&
                StringUtil.isEmpty(mResCanonicalName)
    }

    @Override
    List<String> multipleResCanonicalNames() {
        if (hasMultipleResPart()) {
            mMultipleResCanonicalNames
        } else {
            throw new IllegalStateException(
                    "ModuleDependency ${mModule.projectDir.absolutePath} has no MultipleResPart")
        }
    }

    @Override
    boolean shouldCopy() {
        return false
    }

    @Override
    boolean dstDirExists() {
        return false
    }

    @Override
    void createDstDir() {
        throw new IllegalStateException(
                "ModuleDependency ${mModule.projectDir.absolutePath} don't need copy")
    }

    @Override
    String dstDirAbsolutePath() {
        throw new IllegalStateException(
                "ModuleDependency ${mModule.projectDir.absolutePath} don't need copy")
    }

    @Override
    void copyTo() {
        throw new IllegalStateException(
                "ModuleDependency ${mModule.projectDir.absolutePath} don't need copy")
    }
}