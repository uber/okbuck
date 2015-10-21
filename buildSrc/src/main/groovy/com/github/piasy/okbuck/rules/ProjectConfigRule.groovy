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
import static com.github.piasy.okbuck.helper.CheckUtil.checkListNotEmpty

/**
 * project_config()
 *
 * TODO full buck support
 * */
public final class ProjectConfigRule extends AbstractBuckRule {
    private final String mSrcTarget
    private final List<String> mSrcRoots

    public ProjectConfigRule(String srcTarget, List<String> srcRoots) {
        checkStringNotEmpty(srcTarget, "ProjectConfigRule src_target can't be empty.")
        mSrcTarget = srcTarget
        checkListNotEmpty(srcRoots, "ProjectConfigRule src_roots can't be empty.")
        mSrcRoots = srcRoots
    }

    @Override
    public final void print(PrintStream printer) {
        printer.println("project_config(")
        printer.println("\tsrc_target = '${mSrcTarget}',")
        printer.println("\tsrc_roots = [")
        for (String src : mSrcRoots) {
            printer.println("\t\t'${src}',")
        }
        printer.println("\t],")
        printer.println(")")
        printer.println()
    }
}