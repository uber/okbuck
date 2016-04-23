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

package com.github.piasy.okbuck.rule

/**
 * General presentation for BUCK build rule.
 * */
abstract class BuckRule {

    final String name
    private final String mRuleType
    private final List<String> mVisibility
    private final List<String> mDeps

    BuckRule(String ruleType, String name, List<String> visibility = [], List<String> deps = []) {
        this.name = name
        mRuleType = ruleType
        mVisibility = visibility
        mDeps = deps
    }

    /**
     * Print this rule into the printer.
     */
    void print(PrintStream printer) {
        printer.println("${mRuleType}(")
        printer.println("\tname = '${name}',")
        printContent(printer)
        if (!mDeps.empty) {
            printer.println("\tdeps = [")
            mDeps.each { String dep ->
                printer.println("\t\t'${dep}',")
            }
            printer.println("\t],")
        }
        if (!mVisibility.empty) {
            printer.println("\tvisibility = [")
            for (String visibility : mVisibility) {
                printer.println("\t\t'${visibility}',")
            }
            printer.println("\t],")
        }
        printer.println(")")
        printer.println()
    }

    /**
     * Print rule content.
     *
     * @param printer The printer.
     */
    protected abstract void printContent(PrintStream printer)
}
