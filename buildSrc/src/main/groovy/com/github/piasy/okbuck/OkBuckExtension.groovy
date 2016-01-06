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
package com.github.piasy.okbuck

/**
 * okbuck dsl.
 * */
public class OkBuckExtension {
    /**
     * target: equals to compileSdkVersion in build.gradle.
     * */
    String target = "android-23"

    /**
     * signConfigName: pick one of multiple signing config defined in build.gradle by name.
     * */
    String signConfigName = ""

    /**
     * keystoreDir: directory OkBuck will use to put generated signing config BUCK.
     * */
    String keystoreDir = ".okbuck${File.separator}keystore"

    /**
     * overwrite: overwrite existing BUCK script or not.
     * */
    boolean overwrite = false

    /**
     * whether check dependencies conflict.
     * */
    boolean checkDepConflict = false

    /**
     * resPackages: set the resources package name for Android library module or application module,
     * including string resources, color resources, etc, and BuildConfig.java.
     * */
    Map<String, String> resPackages

    /**
     * linearAllocHardLimit used for multi-dex support.
     * */
    int linearAllocHardLimit = 65535

    /**
     * primary dex class patterns.
     * */
    List<String> primaryDexPatterns = new ArrayList<>()

    /**
     * whether enable exopackage.
     * */
    boolean exopackage = false

    /**
     * exopackage app class source.
     * */
    String appClassSource = ""

    /**
     * exopackage app lib dependencies.
     * */
    List<String> appLibDependencies = new ArrayList<>()

}