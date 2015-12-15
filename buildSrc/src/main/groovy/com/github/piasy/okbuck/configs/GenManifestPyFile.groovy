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

package com.github.piasy.okbuck.configs

/**
 * BUCK file for third party dependencies.
 * */
public final class GenManifestPyFile extends BuckConfigFile {

    @Override
    public final void print(PrintStream printer) {
        printer.println("#!/usr/bin/python")
        printer.println("# -*- coding: utf-8 -*-")
        printer.println("# Thanks for [hujin1860@gmail.com]")
        printer.println("import sys")
        printer.println("import xml.etree.ElementTree as ET")

        printer.println("package = '{http://schemas.android.com/apk/res/android}'")

        printer.println("manifest = ET.parse(sys.argv[1])")
        printer.println("versionName = sys.argv[3]")
        printer.println("versionCode = sys.argv[4]")
        printer.println("minSdk = sys.argv[5]")
        printer.println("targetSdk = sys.argv[6]")
        printer.println("debuggable = sys.argv[7]")

        printer.println("root = manifest.getroot()")
        printer.println("if '{0}versionName'.format(package) not in root.attrib:")
        printer.println("\troot.attrib['{0}versionName'.format(package)] = versionName")

        printer.println("if '{0}versionCode'.format(package) not in root.attrib:")
        printer.println("\troot.attrib['{0}versionCode'.format(package)] = versionCode")

        printer.println("if len(root.findall('./uses-sdk')) == 0:")
        printer.println("\tET.SubElement(root, 'uses-sdk', {")
        printer.println("\t\t'{0}targetSdkVersion'.format(package): targetSdk,")
        printer.println("\t\t'{0}minSdkVersion'.format(package): minSdk,")
        printer.println("\t})")

        printer.println("application = root.find('./application')")
        printer.println("if '{0}debuggable'.format(package) not in application.attrib:")
        printer.println("\tapplication.attrib['{0}debuggable'.format(package)] = debuggable")

        printer.println("manifest.write(sys.argv[2])")

        printer.println("f = open(sys.argv[2], 'r')")
        printer.println("content = f.read()")
        printer.println("f.close()")
        printer.println("content = content.replace('ns0:', 'android:')")
        printer.println("content = content.replace('xmlns:ns0=', 'xmlns:android=')")
        printer.println("f = open(sys.argv[2], 'w')")
        printer.println("f.write(content)")
        printer.println("f.close()")
    }
}