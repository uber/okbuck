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

import org.apache.commons.lang.StringUtils

/**
 * android_library()
 *
 * TODO full buck support
 * */
final class AndroidLibraryRule extends BuckRule {
    private final Set<String> mSrcSet
    private final String mManifest
    private final List<String> mAnnotationProcessors
    private final List<String> mAptDeps
    private final List<String> mAidlRuleNames
    private final String mAppClass
    private final String mSourceCompatibility
    private final String mTargetCompatibility
    private final List<String> mPostprocessClassesCommands

    AndroidLibraryRule(
            String name, List<String> visibility, List<String> deps, Set<String> srcSet,
            String manifest, List<String> annotationProcessors, List<String> aptDeps,
            List<String> aidlRuleNames, String appClass, String sourceCompatibility,
            String targetCompatibility, List<String> postprocessClassesCommands) {
        super("android_library", name, visibility, deps)

        mSrcSet = srcSet
        mManifest = manifest
        mAnnotationProcessors = annotationProcessors
        mAptDeps = aptDeps
        mAidlRuleNames = aidlRuleNames
        mAppClass = appClass
        mSourceCompatibility = sourceCompatibility
        mTargetCompatibility = targetCompatibility
        mPostprocessClassesCommands = postprocessClassesCommands
    }

    @Override
    protected final void printContent(PrintStream printer) {
        printer.println("\tsrcs = glob([")
        for (String src : mSrcSet) {
            printer.println("\t\t'${src}/**/*.java',")
        }

        if (mAppClass != null) {
            printer.println("\t], excludes = ['${mAppClass}']),")
        } else {
            printer.println("\t]),")
        }

        if (!StringUtils.isEmpty(mManifest)) {
            printer.println("\tmanifest = '${mManifest}',")
        }

        if (!mAnnotationProcessors.empty) {
            printer.println("\tannotation_processors = [")
            for (String processor : mAnnotationProcessors) {
                printer.println("\t\t'${processor}',")
            }
            printer.println("\t],")
        }

        if (!mAptDeps.empty) {
            printer.println("\tannotation_processor_deps = [")
            for (String processor : mAptDeps) {
                printer.println("\t\t'${processor}',")
            }
            printer.println("\t],")
        }

        if (!mAidlRuleNames.empty) {
            printer.println("\texported_deps = [")
            mAidlRuleNames.each { String aidlRuleName ->
                printer.println("\t\t'${aidlRuleName}',")
            }
            printer.println("\t],")
        }

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
