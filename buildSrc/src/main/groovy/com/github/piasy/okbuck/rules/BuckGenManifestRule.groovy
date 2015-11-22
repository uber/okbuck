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

package com.github.piasy.okbuck.rules

import com.github.piasy.okbuck.rules.base.BuckRule

import static com.github.piasy.okbuck.helper.CheckUtil.checkStringNotEmpty

/**
 * keystore()
 * */
public final class BuckGenManifestRule extends BuckRule {
    private final String mSrc
    private final String mOut
    private final String mVersionName
    private final int mVersionCode
    private final int mMinSdkVersion
    private final int mTargetSdkVersion
    private final boolean mDebuggable

    public BuckGenManifestRule(
            String name, String src, String out, String versionName, int versionCode,
            int minSdkVersion, int targetSdkVersion, boolean debuggable
    ) {
        super("genrule", name, Collections.emptyList())
        checkStringNotEmpty(src, "BuckGenManifestRule src can't be empty.")
        mSrc = src
        checkStringNotEmpty(out, "BuckGenManifestRule out can't be empty.")
        mOut = out
        checkStringNotEmpty(versionName, "BuckGenManifestRule versionName can't be empty.")
        mVersionName = versionName
        mVersionCode = versionCode
        mMinSdkVersion = minSdkVersion
        mTargetSdkVersion = targetSdkVersion
        mDebuggable = debuggable
    }

    @Override
    protected final void printDetail(PrintStream printer) {
        printer.println("\tsrcs = [")
        printer.println("\t\t'${mSrc}',")
        printer.println("\t],")
        printer.println("\tout = '${mOut}',")
        printer.println("\tbash = '\$(exe //okbuck-scripts:manifest) \$SRCDIR/${mSrc} " +
                "\$OUT ${mVersionName} ${mVersionCode} ${mMinSdkVersion} " +
                "${mTargetSdkVersion} ${mDebuggable}',")
    }
}