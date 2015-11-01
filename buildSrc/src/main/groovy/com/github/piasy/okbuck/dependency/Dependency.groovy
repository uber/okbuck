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

import static com.github.piasy.okbuck.helper.CheckUtil.checkStringNotEmpty

/**
 * Dependency presentation.
 * */
public abstract class Dependency {

    protected final String mSrcCanonicalName

    protected final File mDepFile

    public Dependency(String srcCanonicalName, File depFile) {
        checkStringNotEmpty(srcCanonicalName, "srcCanonicalName can not be empty")
        mSrcCanonicalName = srcCanonicalName
        mDepFile = depFile
    }

    public abstract boolean hasFlavor()

    public String srcCanonicalName() {
        return mSrcCanonicalName
    }

    public File getDepFile() {
        return mDepFile
    }

    public abstract boolean hasResPart()

    public abstract String resCanonicalName()

    public abstract boolean hasMultipleResPart()

    public abstract List<String> multipleResCanonicalNames()

    public abstract boolean shouldCopy()

    public abstract boolean dstDirExists()

    public abstract void createDstDir()

    public abstract String dstDirAbsolutePath()

    public abstract void copyTo()
}