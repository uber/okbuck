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

import com.github.piasy.okbuck.helper.IOHelper

/**
 * remote dependency: jar/aar file.
 * */
public final class RemoteDependency extends Dependency {

    private final File mDstDir

    private final String mResCanonicalName

    public RemoteDependency(
            File depFile, File dstDir, String srcCanonicalName,
            String resCanonicalName
    ) {
        super(srcCanonicalName, depFile)
        mDstDir = dstDir
        mResCanonicalName = resCanonicalName
    }

    @Override
    public String toString() {
        return mDepFile.absolutePath
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof RemoteDependency &&
                ((RemoteDependency) o).mSrcCanonicalName.equals(mSrcCanonicalName)
    }

    @Override
    public int hashCode() {
        return mSrcCanonicalName.hashCode()
    }

    @Override
    boolean hasResPart() {
        return mDepFile.name.endsWith(".aar")
    }

    @Override
    String resCanonicalName() {
        if (hasResPart()) {
            return mResCanonicalName
        } else {
            throw new IllegalStateException(
                    "RemoteDependency ${mDepFile.absolutePath} has no res part")
        }
    }

    @Override
    boolean hasMultipleResPart() {
        return false
    }

    @Override
    List<String> multipleResCanonicalNames() {
        throw new IllegalStateException(
                "RemoteDependency ${mDepFile.absolutePath} has no MultipleResPart")
    }

    @Override
    boolean shouldCopy() {
        return true
    }

    @Override
    boolean dstDirExists() {
        return mDstDir.exists()
    }

    @Override
    void createDstDir() {
        mDstDir.mkdirs()
    }

    @Override
    String dstDirAbsolutePath() {
        return mDstDir.absolutePath
    }

    @Override
    void copyTo() {
        if (!dstDirExists()) {
            mDstDir.mkdirs()
        }
        println "copying ${mDepFile.absolutePath} into ${mDstDir.absolutePath}"
        IOHelper.copy(new FileInputStream(mDepFile), new FileOutputStream(
                new File(mDstDir.absolutePath + File.separator + mDepFile.name)))
    }
}