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
 * java_library()
 *
 * TODO full buck support
 * */
final class JavaLibraryRule extends BuckRule {
    private final Set<String> mSrcSet
    private final Set<String> mAnnotationProcessors
    private final Set<String> mAnnotationProcessorDeps
    private final boolean mRetroLambdaEnabled

    JavaLibraryRule(String name, List<String> visibility, List<String> deps,
                    Set<String> srcSet, Set<String> annotationProcessors,
                    Set<String> annotationProcessorDeps, boolean retroLambdaEnabled
    ) {
        super("java_library", name, visibility, deps)

        mSrcSet = srcSet
        mAnnotationProcessors = annotationProcessors
        mAnnotationProcessorDeps = annotationProcessorDeps
        mRetroLambdaEnabled = retroLambdaEnabled
    }

    @Override
    protected final void printContent(PrintStream printer) {
        printer.println("\tsrcs = glob([")
        for (String src : mSrcSet) {
            printer.println("\t\t'${src}/**/*.java',")
        }
        printer.println("\t]),")

        if (!mAnnotationProcessors.empty) {
            printer.println("\tannotation_processors = [")
            mAnnotationProcessors.each { String processor ->
                printer.println("\t\t'${processor}',")
            }
            printer.println("\t],")
        }

        if (!mAnnotationProcessorDeps.empty) {
            printer.println("\tannotation_processor_deps = [")
            for (String dep : mAnnotationProcessorDeps) {
                printer.println("\t\t'${dep}',")
            }
            printer.println("\t],")
        }

        if (mRetroLambdaEnabled) {
            printer.println("\tsource = '8',")
            printer.println("\ttarget = '8',")
            printer.println("\tpostprocess_classes_commands = ['./.okbuck/RetroLambda/RetroLambda.sh'],")
        }
    }
}
