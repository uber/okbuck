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

package com.github.piasy.okbuck.generator

import com.github.piasy.okbuck.dependency.Dependency
import com.github.piasy.okbuck.dependency.DependencyAnalyzer
import com.github.piasy.okbuck.dependency.DependencyProcessor
import com.github.piasy.okbuck.generator.configs.BUCKFile
import com.github.piasy.okbuck.helper.ProjectHelper
import com.github.piasy.okbuck.helper.StringUtil
import com.github.piasy.okbuck.rules.*
import com.github.piasy.okbuck.rules.base.AbstractBuckRule
import org.gradle.api.Project

import static com.github.piasy.okbuck.helper.CheckUtil.checkStringNotEmpty

/**
 * Created by Piasy{github.com/Piasy} on 15/10/6.
 *
 * X os family generator, Linux, Unix, OS X
 */
public final class XBuckFileGenerator extends BuckFileGenerator {

    public XBuckFileGenerator(
            Project rootProject, DependencyAnalyzer dependencyAnalyzer, File okBuckDir,
            Map<String, String> resPackages, String keystoreDir, String signConfigName,
            String buildVariant
    ) {
        super(rootProject, dependencyAnalyzer, okBuckDir, resPackages, keystoreDir, signConfigName,
                buildVariant)
    }

    @Override
    public Map<Project, BUCKFile> generate() {
        Map<Project, BUCKFile> buckFileMap = new HashMap<>()
        DependencyProcessor dependencyProcessor = new DependencyProcessor(mRootProject,
                mDependencyAnalyzer, mOkBuckDir, mKeystoreDir, mSignConfigName)
        dependencyProcessor.process()

        Map<Project, Set<Dependency>> finalDependenciesGraph = mDependencyAnalyzer.finalDependenciesGraph
        Map<Project, Set<File>> aptDependencies = mDependencyAnalyzer.aptDependencies

        for (Project project : mRootProject.subprojects) {
            List<AbstractBuckRule> rules = new ArrayList<>()
            switch (ProjectHelper.getSubProjectType(project)) {
                case ProjectHelper.ProjectType.AndroidAppProject:
                    createAndroidAppRules(project, finalDependenciesGraph, rules, aptDependencies)
                    break
                case ProjectHelper.ProjectType.AndroidLibProject:
                    createAndroidLibraryRules(project, finalDependenciesGraph, rules, true)
                    break
                case ProjectHelper.ProjectType.JavaLibProject:
                    createJavaLibraryRules(finalDependenciesGraph, project, aptDependencies, rules)
                    break
                case ProjectHelper.ProjectType.Unknown:
                default:
                    break
            }

            if (!rules.empty) {
                buckFileMap.put(project, new BUCKFile(rules))
            } // else, `:libraries:common` will create a sub project `:libraries`, which is garbage
        }

        return buckFileMap
    }

    private void createJavaLibraryRules(
            Map<Project, Set<Dependency>> finalDependenciesGraph, Project project,
            Map<Project, Set<File>> aptDependencies, List<AbstractBuckRule> rules
    ) {
        List<String> deps = new ArrayList<>()
        for (Dependency dependency : finalDependenciesGraph.get(project)) {
            deps.add(dependency.srcCanonicalName)
        }

        Set<String> mainSrcSet = ProjectHelper.getProjectMainSrcSet(project)
        Set<String> srcSet = new HashSet<>()
        for (String srcDir : mainSrcSet) {
            srcSet.add(srcDir + "/**/*.java")
        }

        List<String> annotationProcessorDeps = new ArrayList<>()
        annotationProcessorDeps.add("//" +
                mOkBuckDir.name + "/annotation_processor_deps" +
                ProjectHelper.getPathDiff(mRootProject, project) +
                ":all_jars")

        rules.add(new JavaLibraryRule(Arrays.asList("PUBLIC"), deps, srcSet,
                mDependencyAnalyzer.annotationProcessors.get(project).asList(),
                annotationProcessorDeps))

        rules.add(new ProjectConfigRule("/${ProjectHelper.getPathDiff(mRootProject, project)}:src",
                mainSrcSet.toList()))
    }

    private void createAndroidLibraryRules(
            Project project, Map<Project, Set<Dependency>> finalDependenciesGraph,
            List<AbstractBuckRule> rules, boolean includeManifest
    ) {
        checkStringNotEmpty(mResPackages.get(project.name),
                "resPackage key-value pair must be set for sub project ${project.name}");

        // TODO support multiple res set
        String resDir = ProjectHelper.getProjectMainResDir(project)
        if (!StringUtil.isEmpty(resDir)) {
            List<String> resDeps = new ArrayList<>()
            for (Dependency dependency : finalDependenciesGraph.get(project)) {
                if (dependency.hasResPart()) {
                    resDeps.add(dependency.resCanonicalName)
                }
            }
            String assetsDir = ProjectHelper.getProjectMainAssetsDir(project)
            rules.add(new AndroidResourceRule(Arrays.asList("PUBLIC"), resDeps, resDir,
                    mResPackages.get(project.name), assetsDir))
        }

        rules.add(
                new AndroidBuildConfigRule(Arrays.asList("PUBLIC"), mResPackages.get(project.name),
                        ProjectHelper.getDefaultConfigBuildConfigField(project)))

        List<String> deps = new ArrayList<>()
        deps.add(":build_config")
        if (!StringUtil.isEmpty(resDir)) {
            deps.add(":res")
        }
        for (Dependency dependency : finalDependenciesGraph.get(project)) {
            deps.add(dependency.srcCanonicalName)
            if (dependency.hasResPart() && !dependency.srcCanonicalName.equals(
                    dependency.resCanonicalName)) {
                deps.add(dependency.resCanonicalName)
            }
        }

        Set<String> mainSrcSet = ProjectHelper.getProjectMainSrcSet(project)
        Set<String> srcSet = new HashSet<>()
        for (String srcDir : mainSrcSet) {
            srcSet.add(srcDir + "/**/*.java")
        }

        List<String> annotationProcessorDeps = new ArrayList<>()
        annotationProcessorDeps.add("//" + mOkBuckDir.name +
                "/annotation_processor_deps" +
                ProjectHelper.getPathDiff(mRootProject, project) +
                ":all_jars")
        annotationProcessorDeps.add("//" +
                mOkBuckDir.name + "/annotation_processor_deps" +
                ProjectHelper.getPathDiff(mRootProject, project) +
                ":all_aars")

        rules.add(new AndroidLibraryRule(Arrays.asList("PUBLIC"), deps, srcSet,
                includeManifest ? ProjectHelper.getProjectMainManifestFile(project) : null,
                mDependencyAnalyzer.annotationProcessors.get(project).asList(),
                annotationProcessorDeps))

        rules.add(new ProjectConfigRule("/${ProjectHelper.getPathDiff(mRootProject, project)}:src",
                mainSrcSet.toList()))
    }

    private void createAndroidAppRules(
            Project project, Map<Project, Set<Dependency>> finalDependenciesGraph,
            List<AbstractBuckRule> rules, Map<Project, Set<File>> aptDependencies
    ) {
        createAndroidLibraryRules(project, finalDependenciesGraph, rules, false)

        List<String> manifestDeps = new ArrayList<>()
        for (Dependency dependency : finalDependenciesGraph.get(project)) {
            Project internalDep = dependency.internalDependency(mRootProject)
            if (internalDep != null) {
                // internal android lib/app module dependency
                switch (ProjectHelper.getSubProjectType(internalDep)) {
                    case ProjectHelper.ProjectType.AndroidAppProject:
                    case ProjectHelper.ProjectType.AndroidLibProject:
                        manifestDeps.add(dependency.srcCanonicalName)
                        break
                    case ProjectHelper.ProjectType.JavaLibProject:
                    default:
                        break
                }
            } else {
                if (dependency.hasResPart()) {
                    // aar dependencies
                    manifestDeps.add(dependency.srcCanonicalName)
                }
            }
        }

        rules.add(new AndroidManifestRule(Arrays.asList("PUBLIC"), manifestDeps,
                ProjectHelper.getProjectMainManifestFile(project)))

        rules.add(new AndroidBinaryRule(Arrays.asList("PUBLIC"), Arrays.asList(":res", ":src"),
                ":manifest",
                "//${mKeystoreDir}${ProjectHelper.getPathDiff(mRootProject, project)}:key_store"))
    }
}
