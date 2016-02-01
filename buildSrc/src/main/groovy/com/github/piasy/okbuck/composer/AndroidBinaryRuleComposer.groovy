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

package com.github.piasy.okbuck.composer

import com.github.piasy.okbuck.helper.ProjectHelper
import com.github.piasy.okbuck.helper.StringUtil
import com.github.piasy.okbuck.rules.AndroidBinaryRule
import org.gradle.api.Project

public final class AndroidBinaryRuleComposer {

    private AndroidBinaryRuleComposer() {
        // no instance
    }

    public static AndroidBinaryRule compose(
            Project project, String flavor, String variant, boolean exopackage,
            int linearAllocHardLimit, List<String> primaryDexPatterns
    ) {
        List<String> binDeps = new ArrayList<>()
        binDeps.add(":src_${flavor}_${variant}")
        if (!StringUtil.isEmpty(ProjectHelper.getProjectResDir(project, "main"))) {
            binDeps.add(":res_main")
        }
        if (!StringUtil.isEmpty(ProjectHelper.getProjectResDir(project, variant))) {
            binDeps.add(":res_${variant}")
        }
        if (!StringUtil.isEmpty(ProjectHelper.getProjectResDir(project, flavor))) {
            binDeps.add(":res_${flavor}")
        }
        if (!StringUtil.isEmpty(
                ProjectHelper.getProjectResDir(project, flavor + variant.capitalize()))) {
            binDeps.add(":res_${flavor}_${variant}")
        }
        if (exopackage) {
            binDeps.add(":app_lib_${flavor}_${variant}")
        }
        if (ProjectHelper.getMultiDexEnabled(project)) {
            return new AndroidBinaryRule("bin_${flavor}_${variant}", Arrays.asList("PUBLIC"),
                    binDeps, ":manifest", "//.okbuck/${project.name}_keystore:key_store_${variant}",
                    linearAllocHardLimit, primaryDexPatterns, exopackage)
        } else {
            return new AndroidBinaryRule("bin_${flavor}_${variant}", Arrays.asList("PUBLIC"),
                    binDeps, ":manifest", "//.okbuck/${project.name}_keystore:key_store_${variant}",
                    exopackage)
        }
    }

    public static AndroidBinaryRule composeWithoutFlavor(
            Project project, String variant, boolean exopackage,
            int linearAllocHardLimit, List<String> primaryDexPatterns
    ) {
        List<String> binDeps = new ArrayList<>()
        binDeps.add(":src_${variant}")
        if (!StringUtil.isEmpty(ProjectHelper.getProjectResDir(project, "main"))) {
            binDeps.add(":res_main")
        }
        if (!StringUtil.isEmpty(ProjectHelper.getProjectResDir(project, variant))) {
            binDeps.add(":res_${variant}")
        }
        if (exopackage) {
            binDeps.add(":app_lib_${variant}")
        }
        if (ProjectHelper.getMultiDexEnabled(project)) {
            return new AndroidBinaryRule("bin_${variant}", Arrays.asList("PUBLIC"),
                    binDeps, ":manifest", "//.okbuck/${project.name}_keystore:key_store_${variant}",
                    linearAllocHardLimit, primaryDexPatterns, exopackage)
        } else {
            return new AndroidBinaryRule("bin_${variant}", Arrays.asList("PUBLIC"),
                    binDeps, ":manifest", "//.okbuck/${project.name}_keystore:key_store_${variant}",
                    exopackage)
        }
    }
}