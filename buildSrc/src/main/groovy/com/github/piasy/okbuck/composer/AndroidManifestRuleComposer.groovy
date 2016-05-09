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

import com.github.piasy.okbuck.model.AndroidAppTarget
import com.github.piasy.okbuck.model.AndroidLibTarget
import com.github.piasy.okbuck.model.Target
import com.github.piasy.okbuck.rule.AndroidManifestRule

final class AndroidManifestRuleComposer {

    private AndroidManifestRuleComposer() {
        // no instance
    }

    static AndroidManifestRule compose(AndroidAppTarget target) {
        List<String> deps = []

        deps.addAll(target.compileDeps.findAll { String dep ->
            dep.endsWith("aar")
        }.collect { String dep ->
            "//${dep.reverse().replaceFirst("/", ":").reverse()}"
        })

        deps.addAll(target.targetCompileDeps.findAll { Target targetDep ->
            targetDep instanceof AndroidLibTarget
        }.collect { Target targetDep ->
            "//${targetDep.path}:src_${targetDep.name}"
        })

        return new AndroidManifestRule("manifest_${target.name}", ["PUBLIC"], deps, target.manifest)
    }
}
