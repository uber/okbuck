/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Piasy
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

import com.github.piasy.okbuck.helper.CheckUtil
import com.github.piasy.okbuck.helper.FileUtil
import com.github.piasy.okbuck.helper.IOHelper

/**
 * Dependency presentation.
 * */
public abstract class FileDependency extends Dependency {

    protected FileDependency(DependencyType dependencyType, File localFile, File projectRootDir) {
        super(dependencyType, localFile, projectRootDir)
    }

    @Override
    boolean shouldCopy() {
        return true
    }

    protected File mDstDir

    @Override
    public void setDstDir(File dstDir) {
        CheckUtil.checkNotNull(dstDir, "dstDir can't be null")
        mDstDir = dstDir
    }

    @Override
    File getDstDir() {
        return mDstDir
    }

    @Override
    String getSrcCanonicalName() {
        if (mDstDir == null) {
            throw new NullPointerException("mDstDir should be set before this call")
        }
        return "/${FileUtil.getDirPathDiff(mRootProjectDir, mDstDir) + getBuckDepName()}"
    }

    private String getBuckDepName() {
        switch (getType()) {
            case DependencyType.LocalJarDependency:
            case DependencyType.MavenJarDependency:
                return ":jar__${mLocalFile.name}"
            case DependencyType.LocalAarDependency:
            case DependencyType.MavenAarDependency:
                return ":aar__${mLocalFile.name}"
            default:
                throw new IllegalArgumentException("bad type of ${mLocalFile.name}")
        }
    }

    @Override
    List<String> getResCanonicalNames() {
        switch (getType()) {
            case DependencyType.LocalJarDependency:
            case DependencyType.MavenJarDependency:
                return Collections.emptyList()
            case DependencyType.LocalAarDependency:
            case DependencyType.MavenAarDependency:
                return Collections.singletonList(
                        "/${FileUtil.getDirPathDiff(mRootProjectDir, mDstDir)}" +
                                ":aar__${mLocalFile.name}")
            default:
                throw new IllegalArgumentException("bad type of ${mLocalFile.name}")
        }
    }

    @Override
    void copyTo() {
        if (!mDstDir.exists()) {
            mDstDir.mkdirs()
        }
        logger.debug "copying ${mLocalFile.absolutePath} into ${mDstDir.absolutePath}"
        IOHelper.copy(new FileInputStream(mLocalFile), new FileOutputStream(
                new File("${mDstDir.absolutePath}/${mLocalFile.name}")))
    }
}