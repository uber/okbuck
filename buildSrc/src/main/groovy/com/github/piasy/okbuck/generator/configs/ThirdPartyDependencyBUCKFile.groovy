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

package com.github.piasy.okbuck.generator.configs

/**
 * BUCK file for third party dependencies.
 * */
public final class ThirdPartyDependencyBUCKFile extends BuckConfigFile {

    private final boolean mIncludeShortCut

    public ThirdPartyDependencyBUCKFile(boolean includeShortCut) {
        mIncludeShortCut = includeShortCut
    }

    @Override
    public final void print(PrintStream printer) {
        printer.println("import re")
        printer.println()

        printer.println("jar_deps = []")
        printer.println("for jarfile in glob(['*.jar']):")
        printer.println("\tname = 'jar__' + re.sub(r'^.*/([^/]+)\\.jar\$', r'\\1', jarfile)")
        printer.println("\tjar_deps.append(':' + name)")
        printer.println("\tprebuilt_jar(")
        printer.println("\t\tname = name,")
        printer.println("\t\tbinary_jar = jarfile,")
        printer.println("\t\tvisibility = ['PUBLIC'],")
        printer.println("\t)")
        printer.println()

        printer.println("aar_deps = []")
        printer.println("for aarfile in glob(['*.aar']):")
        printer.println("\tname = 'aar__' + re.sub(r'^.*/([^/]+)\\.aar\$', r'\\1', aarfile)")
        printer.println("\taar_deps.append(':' + name)")
        printer.println("\tandroid_prebuilt_aar(")
        printer.println("\t\tname = name,")
        printer.println("\t\taar = aarfile,")
        printer.println("\t\tvisibility = ['PUBLIC'],")
        printer.println("\t)")
        printer.println()

        if (mIncludeShortCut) {
            printer.println("android_library(")
            printer.println("\tname = 'all_jars',")
            printer.println("\texported_deps = jar_deps,")
            printer.println("\tvisibility = [")
            printer.println("\t\t'PUBLIC',")
            printer.println("\t],")
            printer.println(")")
            printer.println()
            
            printer.println("android_library(")
            printer.println("\tname = 'all_aars',")
            printer.println("\texported_deps = aar_deps,")
            printer.println("\tvisibility = [")
            printer.println("\t\t'PUBLIC',")
            printer.println("\t],")
            printer.println(")")
            printer.println()
        }
    }
}