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
     * build_tools_version
     * */
    String buildToolVersion = "23.0.1"

    /**
     * Android target sdk version
     * */
    String target = "android-23"

    /**
     * overwrite: overwrite existing BUCK script or not.
     * */
    boolean overwrite = false

    /**
     * whether check dependencies conflict.
     * */
    boolean checkDepConflict = true

    /**
     * resPackages: set the resources package name for Android library module or application module,
     * including string resources, color resources, etc, and BuildConfig.java.
     * */
    Map<String, String> resPackages

    /**
     * linearAllocHardLimit used for multi-dex support.
     * */
    Map<String, Integer> linearAllocHardLimit = new HashMap<>()

    /**
     * primary dex class patterns.
     * */
    Map<String, List<String>> primaryDexPatterns = new HashMap<>()

    /**
     * whether enable exopackage.
     * */
    Map<String, Boolean> exopackage = new HashMap<>()

    /**
     * exopackage app class source.
     * */
    Map<String, String> appClassSource = new HashMap<>()

    /**
     * exopackage app lib dependencies.
     * */
    Map<String, List<String>> appLibDependencies = new HashMap<>()

    /**
     * flavor filter, if not empty, only create buck config for listed flavors.
     * */
    Map<String, List<String>> flavorFilter = new HashMap<>()

}