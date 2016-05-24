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

package com.github.okbuilds.okbuck.config

/**
 * .buckconfig file.
 *
 * TODO full buck support
 * */
final class DotBuckConfigLocalFile extends BuckConfigFile {

    private final Map<String, String> mAliases
    private final String mBuildToolVersion
    private final String mTarget
    private final List<String> mIgnore

    DotBuckConfigLocalFile(Map<String, String> aliases, String buildToolVersion, String target, List<String> ignore) {
        mAliases = aliases
        mBuildToolVersion = buildToolVersion
        mTarget = target
        mIgnore = ignore
    }

    @Override
    final void print(PrintStream printer) {
        printer.println("[alias]")
        mAliases.each { alias, target ->
            printer.println("\t${alias} = ${target}")
        }
        printer.println()

        printer.println("[android]")
        printer.println("\tbuild_tools_version = ${mBuildToolVersion}")
        printer.println("\ttarget = ${mTarget}")
        printer.println()

        printer.println("[project]")
        printer.print("\tignore = ${mIgnore.join(', ')}")
        printer.println()
    }
}
