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

import com.github.piasy.okbuck.rules.base.AbstractBuckRule

import static com.github.piasy.okbuck.helper.CheckUtil.checkStringNotEmpty
/**
 * gen_aidl wrapper
 * */
public final class GenAidlRule extends AbstractBuckRule {
    private final String mRuleName
    private final String mAidlFilePath
    private final String mImportPath

    public GenAidlRule(String ruleName, String aidlFilePath, String importPath) {
        mRuleName = ruleName
        checkStringNotEmpty(aidlFilePath, "GenAidlRule aidlFilePath can't be empty.")
        mAidlFilePath = aidlFilePath
        checkStringNotEmpty(importPath, "GenAidlRule importPath can't be empty.")
        mImportPath = importPath
    }

    public String getRuleName() {
        return mRuleName
    }

    @Override
    public final void print(PrintStream printer) {
        printer.println("import re")
        printer.println("gen_${mRuleName} = []")
        printer.println("for aidl_file in glob(['${mAidlFilePath.substring(1)}/**/*.aidl']):")
        printer.println("\tname = '${mRuleName}__' + re.sub(r'^.*/([^/]+)\\.aidl\$', r'\\1', aidl_file)")
        printer.println("\tgen_${mRuleName}.append(':' + name)")
        printer.println("\tgen_aidl(")
        printer.println("\t\tname = name,")
        printer.println("\t\taidl = aidl_file,")
        printer.println("\t\timport_path = '${mImportPath}',")
        printer.println("\t)")
        printer.println()

        printer.println("android_library(")
        printer.println("\tname = '${mRuleName}',")
        printer.println("\tsrcs = gen_${mRuleName},")
        printer.println(")")
        printer.println()
    }
}