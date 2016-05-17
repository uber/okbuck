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

import static com.github.piasy.okbuck.util.CheckUtil.checkNotNull
import static com.github.piasy.okbuck.util.CheckUtil.checkStringNotEmpty

/**
 * android_binary()
 *
 * TODO full buck support
 * */
final class AndroidBinaryRule extends BuckRule {
    private final String mManifest
    private final String mKeystore
    private final boolean mMultidexEnabled
    private final int mLinearAllocHardLimit
    private final Set<String> mPrimaryDexPatterns
    private final boolean mExopackage
    private final Set<String> mCpuFilters
    private final boolean mMinifyEnabled
    private final String mProguardConfig

    AndroidBinaryRule(String name, List<String> visibility, List<String> deps, String manifest, String keystore,
                      boolean multidexEnabled, int linearAllocHardLimit, Set<String> primaryDexPatterns,
                      boolean exopackage, Set<String> cpuFilters, boolean minifyEnabled,
                      String proguardConfig) {
        super("android_binary", name, visibility, deps)

        checkStringNotEmpty(manifest, "AndroidBinaryRule manifest must be set.")
        mManifest = manifest
        checkStringNotEmpty(keystore, "AndroidBinaryRule keystore must be set.")
        mKeystore = keystore
        mMultidexEnabled = multidexEnabled
        mLinearAllocHardLimit = linearAllocHardLimit
        checkNotNull(primaryDexPatterns, "AndroidBinaryRule primaryDexPatterns must be non-null.")
        mPrimaryDexPatterns = primaryDexPatterns
        mExopackage = exopackage
        mCpuFilters = cpuFilters
        mMinifyEnabled = minifyEnabled
        mProguardConfig = proguardConfig
    }

    @Override
    protected final void printContent(PrintStream printer) {
        printer.println("\tmanifest = '${mManifest}',")
        printer.println("\tkeystore = '${mKeystore}',")
        if (mExopackage) {
            printer.println("\texopackage_modes = ['secondary_dex'],")
        }
        if (mMultidexEnabled && mPrimaryDexPatterns != null) {
            printer.println("\tuse_split_dex = True,")
            printer.println("\tlinear_alloc_hard_limit = ${mLinearAllocHardLimit},")
            if (mPrimaryDexPatterns != null && !mPrimaryDexPatterns.empty) {
                printer.println("\tprimary_dex_patterns = [")
                for (String pattern : mPrimaryDexPatterns) {
                    printer.println("\t\t'${pattern}',")
                }
                printer.println("\t],")
            }
        }
        if (mCpuFilters != null && !mCpuFilters.empty) {
            printer.println("\tcpu_filters = [")
            for (String filter : mCpuFilters) {
                printer.println("\t\t'${filter}',")
            }
            printer.println("\t],")
        }
        if (mMinifyEnabled) {
            printer.println("\tpackage_type = 'release',")
            printer.println("\tandroid_sdk_proguard_config = 'none',")
            printer.println("\tproguard_config = '${mProguardConfig}',")
        }
    }
}
