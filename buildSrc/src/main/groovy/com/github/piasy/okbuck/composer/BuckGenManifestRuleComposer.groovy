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
import com.github.piasy.okbuck.rules.BuckGenManifestRule
import org.gradle.api.Project

public final class BuckGenManifestRuleComposer {

    private BuckGenManifestRuleComposer() {
        // no instance
    }

    public static BuckGenManifestRule compose(Project project) {
        return new BuckGenManifestRule("generate_manifest_main",
                ProjectHelper.getProjectManifestFile(project, "main"), "AndroidManifest.xml",
                ProjectHelper.getVersionName(project, "default"),
                ProjectHelper.getVersionCode(project, "default"),
                ProjectHelper.getMinSdkVersion(project, "default"),
                ProjectHelper.getTargetSdkVersion(project, "default"), true)
    }
}