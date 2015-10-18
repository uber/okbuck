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

import com.github.piasy.okbuck.rules.base.BuckRuleWithDeps

import static com.github.piasy.okbuck.helper.CheckUtil.checkNotEmpty
import static com.github.piasy.okbuck.helper.CheckUtil.checkNotNull

/**
 * java_library()
 *
 * TODO full buck support
 * */
public final class JavaLibraryRule extends BuckRuleWithDeps {
    private final Set<String> mSrcSet
    private final List<String> mAnnotationProcessors
    private final List<String> mAnnotationProcessorDeps

    public JavaLibraryRule(
            List<String> visibility, List<String> deps, Set<String> srcSet,
            List<String> annotationProcessors, List<String> annotationProcessorDeps
    ) {
        super("java_library", "src", visibility, deps)

        checkNotEmpty(srcSet, "JavaLibraryRule srcs must be non-null.")
        mSrcSet = srcSet
        checkNotNull(annotationProcessors,
                "JavaLibraryRule annotation_processors must be non-null.")
        mAnnotationProcessors = annotationProcessors
        checkNotNull(annotationProcessorDeps,
                "JavaLibraryRule annotation_processor_deps must be non-null.")
        mAnnotationProcessorDeps = annotationProcessorDeps
    }

    @Override
    protected final void printSpecificPart(PrintStream printer) {
        printer.println("\tsrcs = glob([")
        for (String src : mSrcSet) {
            printer.println("\t\t'${src}',")
        }
        printer.println("\t]),")

        printer.println("\tannotation_processors = [")
        for (String processor : mAnnotationProcessors) {
            printer.println("\t\t'${processor}',")
        }
        printer.println("\t],")

        printer.println("\tannotation_processor_deps = [")
        for (String dep : mAnnotationProcessorDeps) {
            printer.println("\t\t'${dep}',")
        }
        printer.println("\t],")
    }
}