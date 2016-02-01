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

import com.github.piasy.okbuck.dependency.Dependency
import com.github.piasy.okbuck.helper.ProjectHelper
import com.github.piasy.okbuck.helper.StringUtil
import com.github.piasy.okbuck.rules.AndroidLibraryRule
import org.gradle.api.Project

public final class AndroidLibraryRuleComposer {

    private AndroidLibraryRuleComposer() {
        // no instance
    }

    /**
     *release
     * */
    public static AndroidLibraryRule compose4LibraryWithoutFlavor(
            String ruleName, Project project, File okbuckDir, Set<Dependency> finalDependencies,
            Set<String> annotationProcessors, boolean isForLibraryModule, boolean excludeAppClass
    ) {
        return compose(ruleName, project, okbuckDir, finalDependencies, annotationProcessors,
                null, "release", ":build_config", isForLibraryModule, excludeAppClass)
    }

    /**
     * variant
     * */
    public static AndroidLibraryRule compose4AppWithoutFlavor(
            String ruleName, Project project, File okbuckDir, Set<Dependency> finalDependencies,
            Set<String> annotationProcessors, String variant, boolean isForLibraryModule,
            boolean excludeAppClass
    ) {
        return compose(ruleName, project, okbuckDir, finalDependencies, annotationProcessors,
                null, variant, ":build_config_${variant}", isForLibraryModule, excludeAppClass)
    }

    /**
     * flavor_variant
     * */
    public static AndroidLibraryRule composeWithFlavor(
            String ruleName, Project project, File okbuckDir, Set<Dependency> finalDependencies,
            Set<String> annotationProcessors, String flavor, String variant,
            boolean isForLibraryModule, boolean excludeAppClass
    ) {
        return compose(ruleName, project, okbuckDir, finalDependencies, annotationProcessors,
                flavor, variant, ":build_config_${flavor}_${variant}", isForLibraryModule,
                excludeAppClass)
    }

    private static AndroidLibraryRule compose(
            String ruleName, Project project, File okbuckDir, Set<Dependency> finalDependencies,
            Set<String> annotationProcessors, String flavor, String variant,
            String buildConfigRuleName, boolean isForLibraryModule, boolean excludeAppClass
    ) {
        Set<String> srcSet = new HashSet<>()
        for (String srcDir : ProjectHelper.getProjectSrcSet(project, "main")) {
            srcSet.add(srcDir + "/**/*.java")
        }
        for (String srcDir : ProjectHelper.getProjectSrcSet(project, variant)) {
            srcSet.add(srcDir + "/**/*.java")
        }
        if (!StringUtil.isEmpty(flavor)) {
            for (String srcDir : ProjectHelper.getProjectSrcSet(project, flavor)) {
                srcSet.add(srcDir + "/**/*.java")
            }
            for (String srcDir :
                    ProjectHelper.getProjectSrcSet(project, flavor + variant.capitalize())) {
                srcSet.add(srcDir + "/**/*.java")
            }
        }

        List<String> deps = new ArrayList<>()
        String resDir = ProjectHelper.getProjectResDir(project, "main")
        if (!StringUtil.isEmpty(resDir)) {
            deps.add(":res_main")
        }
        resDir = ProjectHelper.getProjectResDir(project, variant)
        if (!StringUtil.isEmpty(resDir)) {
            deps.add(":res_${variant}")
        }
        if (!StringUtil.isEmpty(flavor)) {
            resDir = ProjectHelper.getProjectResDir(project, flavor)
            if (!StringUtil.isEmpty(resDir)) {
                deps.add(":res_${flavor}")
            }
            resDir = ProjectHelper.getProjectResDir(project, flavor + variant.capitalize())
            if (!StringUtil.isEmpty(resDir)) {
                deps.add(":res_${flavor}_${variant}")
            }
        }

        for (Dependency dependency : finalDependencies) {
            deps.add(dependency.srcCanonicalName)
            for (String resName : dependency.resCanonicalNames) {
                if (!dependency.srcCanonicalName.equals(resName)) {
                    deps.add(resName)
                }
            }
        }

        deps.add(buildConfigRuleName)
        String jniLibsDir = ProjectHelper.getProjectJniLibsDir(project, "main")
        if (!StringUtil.isEmpty(jniLibsDir)) {
            deps.add(":native_libs")
        }

        String manifest = isForLibraryModule ?
                ProjectHelper.getProjectManifestFile(project, "main") : null

        List<String> annotationProcessorDeps
        if (annotationProcessors.empty) {
            annotationProcessorDeps = Collections.emptyList()
        } else {
            annotationProcessorDeps = new ArrayList<>()
            annotationProcessorDeps.add("//${okbuckDir.name}/${project.name}_apt_deps:all_jars")
            annotationProcessorDeps.add("//${okbuckDir.name}/${project.name}_apt_deps:all_aars")
        }

        return new AndroidLibraryRule(ruleName, Arrays.asList("PUBLIC"), deps, srcSet,
                manifest, annotationProcessors.asList(), annotationProcessorDeps, excludeAppClass)
    }
}