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

package com.github.piasy.okbuck.rule

/**
 * android_library() used for exopackage
 * */
final class ExopackageAndroidLibraryRule extends BuckRule {
    private final String mAppClass
    private final String mSourceCompatibility
    private final String mTargetCompatibility
    private final List<String> mPostprocessClassesCommands

    ExopackageAndroidLibraryRule(String name, String appClass, List<String> visibility,
                                 List<String> deps, String sourceCompatibility,
                                 String targetCompatibility,
                                 List<String> postprocessClassesCommands) {
        super("android_library", name, visibility, deps)
        mAppClass = appClass
        mSourceCompatibility = sourceCompatibility
        mTargetCompatibility = targetCompatibility
        mPostprocessClassesCommands = postprocessClassesCommands
    }

    @Override
    protected final void printContent(PrintStream printer) {
        printer.println("\tsrcs = ['${mAppClass}'],")
        printer.println("\tsource = '${mSourceCompatibility}',")
        printer.println("\ttarget = '${mTargetCompatibility}',")
        if (!mPostprocessClassesCommands.empty) {
            printer.println("\tpostprocess_classes_commands = [")
            mPostprocessClassesCommands.each { String command ->
                printer.println("\t\t'${command}',")
            }
            printer.println("\t],")
        }
    }
}
