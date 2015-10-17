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

package com.github.piasy.okbuck.rules.base

import static com.github.piasy.okbuck.helper.CheckUtil.checkNotNull

/**
 * General presentation for BUCK build rule with deps part.
 * */
abstract class BuckRuleWithDeps extends BuckRule {
    private final List<String> mDeps

    protected BuckRuleWithDeps(
            String ruleType, String name, List<String> visibility, List<String> deps
    ) {
        super(ruleType, name, visibility)
        checkNotNull(deps, "BuckRuleWithDeps deps must be non-null.")
        mDeps = deps
    }

    @Override
    protected final void printDetail(PrintStream printer) {
        printSpecificPart(printer)
        if (!mDeps.empty) {
            printer.println("\tdeps = [")
            for (String dep : mDeps) {
                printer.println("\t\t'${dep}',")
            }
            printer.println("\t],")
        }
    }

    /**
     * print the rule specific part
     * */
    protected abstract void printSpecificPart(PrintStream printer)
}