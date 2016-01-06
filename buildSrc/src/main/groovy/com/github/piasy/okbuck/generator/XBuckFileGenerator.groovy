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

import com.github.piasy.okbuck.configs.BUCKFile
import com.github.piasy.okbuck.dependency.Dependency
import com.github.piasy.okbuck.dependency.DependencyAnalyzer
import com.github.piasy.okbuck.dependency.DependencyProcessor
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
            int linearAllocHardLimit, List<String> primaryDexPatterns, boolean exopackage,
            String appClassSource, List<String> appLibDependencies
    ) {
        super(rootProject, dependencyAnalyzer, okBuckDir, resPackages, keystoreDir, signConfigName,
                linearAllocHardLimit, primaryDexPatterns, exopackage, appClassSource,
                appLibDependencies)
    }

    @Override
    public Map<Project, BUCKFile> generate() {
        Map<Project, BUCKFile> buckFileMap = new HashMap<>()
        DependencyProcessor dependencyProcessor = new DependencyProcessor(mRootProject,
                mDependencyAnalyzer, mOkBuckDir, mKeystoreDir, mSignConfigName)
        dependencyProcessor.process()

        Map<Project, Map<String, Set<Dependency>>> finalDependenciesGraph = mDependencyAnalyzer.finalDependenciesGraph

        for (Project project : mRootProject.subprojects) {
            List<AbstractBuckRule> rules = new ArrayList<>()
            switch (ProjectHelper.getSubProjectType(project)) {
                case ProjectHelper.ProjectType.AndroidAppProject:
                    createAndroidAppRules(project, finalDependenciesGraph, mAppClassSource,
                            matchAppLibDependencies(mAppLibDependencies, finalDependenciesGraph),
                            mExopackage, rules)
                    break
                case ProjectHelper.ProjectType.AndroidLibProject:
                    createAndroidLibraryRules(project, finalDependenciesGraph, rules, true, false)
                    break
                case ProjectHelper.ProjectType.JavaLibProject:
                    createJavaLibraryRules(finalDependenciesGraph.get(project).get("main"), project,
                            rules)
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

    private static List<Dependency> matchAppLibDependencies(
            List<String> deps, Map<Project, Map<String, Set<Dependency>>> finalDependenciesGraph
    ) {
        List<Dependency> dependencies = new ArrayList<>()
        for (String dep : deps) {
            boolean found = false
            for (Project prj : finalDependenciesGraph.keySet()) {
                if (found) {
                    break
                }
                for (String flavor : finalDependenciesGraph.get(prj).keySet()) {
                    if (found) {
                        break
                    }
                    for (Dependency dependency : finalDependenciesGraph.get(prj).get(flavor)) {
                        if (dependency.depFile.name.contains(dep)) {
                            dependencies.add(dependency)
                            found = true
                            break
                        }
                    }
                }
            }
            if (!found) {
                throw new IllegalStateException("App lib's dependency '${dep}' could not be found")
            }
        }
        return dependencies
    }

    private void createJavaLibraryRules(
            Set<Dependency> finalDependencies, Project project,
            List<AbstractBuckRule> rules
    ) {
        List<String> deps = new ArrayList<>()
        for (Dependency dependency : finalDependencies) {
            deps.add(dependency.srcCanonicalName())
        }

        Set<String> mainSrcSet = ProjectHelper.getProjectSrcSet(project, "main")
        Set<String> srcSet = new HashSet<>()
        for (String srcDir : mainSrcSet) {
            srcSet.add(srcDir + "/**/*.java")
        }

        List<String> annotationProcessorDeps = new ArrayList<>()
        annotationProcessorDeps.add("//" +
                mOkBuckDir.name + "/annotation_processor_deps" +
                ProjectHelper.getProjectPathDiff(mRootProject, project) +
                ":all_jars")

        rules.add(new JavaLibraryRule(Arrays.asList("PUBLIC"), deps, srcSet,
                mDependencyAnalyzer.annotationProcessors.get(project).asList(),
                annotationProcessorDeps))

        rules.add(new ProjectConfigRule(
                "/${ProjectHelper.getProjectPathDiff(mRootProject, project)}:src",
                mainSrcSet.toList()))
    }

    private static void createExopackageRules(
            Project project, String appClassSource, List<Dependency> dependencies,
            List<AbstractBuckRule> rules
    ) {
        rules.add(new AppClassSourceRule(appClassSource))
        List<String> deps = new ArrayList<>()
        for (Dependency dependency : dependencies) {
            deps.add(dependency.srcCanonicalName())
        }
        if (ProjectHelper.exportFlavor(project)) {
            for (String flavor : ProjectHelper.getProductFlavors(project).keySet()) {
                List<String> debugDeps = new ArrayList<>(deps)
                debugDeps.add(":build_config_${flavor}_debug")
                rules.add(new ExopackageAndroidLibraryRule("app_lib_${flavor}_debug",
                        Arrays.asList("PUBLIC"), debugDeps))

                List<String> releaseDeps = new ArrayList<>(deps)
                releaseDeps.add(":build_config_${flavor}_release")
                rules.add(new ExopackageAndroidLibraryRule("app_lib_${flavor}_release",
                        Arrays.asList("PUBLIC"), releaseDeps))
            }
        } else {
            List<String> debugDeps = new ArrayList<>(deps)
            debugDeps.add(":build_config_debug")
            rules.add(new ExopackageAndroidLibraryRule("app_lib_debug", Arrays.asList("PUBLIC"),
                    debugDeps))

            List<String> releaseDeps = new ArrayList<>(deps)
            releaseDeps.add(":build_config_release")
            rules.add(new ExopackageAndroidLibraryRule("app_lib_release", Arrays.asList("PUBLIC"),
                    releaseDeps))
        }
    }

    private void createAndroidLibraryRules(
            Project project, Map<Project, Map<String, Set<Dependency>>> finalDependenciesGraph,
            List<AbstractBuckRule> rules, boolean includeManifest, boolean excludeAppClass
    ) {
        checkStringNotEmpty(mResPackages.get(project.name),
                "resPackage key-value pair must be set for sub project ${project.name}");

        // TODO support multiple res set
        if (ProjectHelper.exportFlavor(project)) {
            addAndroidResRule(project, finalDependenciesGraph, rules, "main",
                    "main", "res_main")
            for (String flavor : ProjectHelper.getProductFlavors(project).keySet()) {
                addAndroidResRule(project, finalDependenciesGraph, rules, flavor,
                        "${flavor}_release", "res_${flavor}")
                addAndroidResRule(project, finalDependenciesGraph, rules, "debug",
                        "${flavor}_debug", "res_debug")
                addAndroidResRule(project, finalDependenciesGraph, rules, "release",
                        "${flavor}_release", "res_release")
                addAndroidResRule(project, finalDependenciesGraph, rules, "${flavor}Debug",
                        "${flavor}_debug", "res_${flavor}_debug")
                addAndroidResRule(project, finalDependenciesGraph, rules, "${flavor}Release",
                        "${flavor}_release", "res_${flavor}_release")

                // build config
                rules.add(new AndroidBuildConfigRule("build_config_${flavor}_debug",
                        Arrays.asList("PUBLIC"), mResPackages.get(project.name),
                        ProjectHelper.getBuildConfigField(project, flavor, "debug")))
                rules.add(new AndroidBuildConfigRule("build_config_${flavor}_release",
                        Arrays.asList("PUBLIC"), mResPackages.get(project.name),
                        ProjectHelper.getBuildConfigField(project, flavor, "release")))
            }

            // src rule
            for (String flavor : ProjectHelper.getProductFlavors(project).keySet()) {
                addAndroidLibraryRule(project, finalDependenciesGraph, rules, flavor, "debug",
                        "${flavor}_debug", "src_${flavor}_debug", false, includeManifest,
                        excludeAppClass)
                addAndroidLibraryRule(project, finalDependenciesGraph, rules, flavor, "release",
                        "${flavor}_release", "src_${flavor}_release", false, includeManifest,
                        excludeAppClass)
            }
        } else {
            if (includeManifest) {
                // for android library module
                addAndroidResRule(project, finalDependenciesGraph, rules, "main", "release",
                        "res_main")
                addAndroidResRule(project, finalDependenciesGraph, rules, "release", "release",
                        "res_release")

                // build config
                rules.add(new AndroidBuildConfigRule("build_config", Arrays.asList("PUBLIC"),
                        mResPackages.get(project.name),
                        ProjectHelper.getBuildConfigField(project, "default", "release")))
            } else {
                // for android application module
                addAndroidResRule(project, finalDependenciesGraph, rules, "main", "release",
                        "res_main")
                addAndroidResRule(project, finalDependenciesGraph, rules, "debug", "debug",
                        "res_debug")
                addAndroidResRule(project, finalDependenciesGraph, rules, "release", "release",
                        "res_release")

                // build config
                rules.add(new AndroidBuildConfigRule("build_config_debug", Arrays.asList("PUBLIC"),
                        mResPackages.get(project.name),
                        ProjectHelper.getBuildConfigField(project, "default", "debug")))
                rules.add(
                        new AndroidBuildConfigRule("build_config_release", Arrays.asList("PUBLIC"),
                                mResPackages.get(project.name),
                                ProjectHelper.getBuildConfigField(project, "default", "release")))
            }

            // src rule
            if (includeManifest) {
                // lib
                addAndroidLibraryRule(project, finalDependenciesGraph, rules, null, null, "release",
                        "src", true, includeManifest, excludeAppClass)
            } else {
                // app
                addAndroidLibraryRule(project, finalDependenciesGraph, rules, "main", "debug",
                        "debug", "src_debug", false, includeManifest, excludeAppClass)
                addAndroidLibraryRule(project, finalDependenciesGraph, rules, "main", "release",
                        "release", "src_release", false, includeManifest, excludeAppClass)
            }
        }

        // TODO jni libs multi flavor support
        // jni libs
        String jniLibsDir = ProjectHelper.getProjectJniLibsDir(project, "main")
        if (!StringUtil.isEmpty(jniLibsDir)) {
            rules.add(new PrebuiltNativeLibraryRule(Arrays.asList("PUBLIC"), jniLibsDir))
        }

        rules.add(new ProjectConfigRule(
                "/${ProjectHelper.getProjectPathDiff(mRootProject, project)}:src",
                ProjectHelper.getProjectSrcSet(project, "main").toList()))
    }

    private void addAndroidLibraryRule(
            Project project, Map<Project, Map<String, Set<Dependency>>> finalDependenciesGraph,
            List<AbstractBuckRule> rules, String selfFlavor, String selfVariant,
            String depsOfFlavor, String ruleName, boolean defaultFlavor, boolean includeManifest,
            boolean excludeAppClass
    ) {
        List<String> deps = new ArrayList<>()
        Set<String> srcSet = new HashSet<>()
        if (defaultFlavor) {
            String resDir = ProjectHelper.getProjectResDir(project, "main")
            if (!StringUtil.isEmpty(resDir)) {
                deps.add(":res_main")
            }
            resDir = ProjectHelper.getProjectResDir(project, "release")
            if (!StringUtil.isEmpty(resDir)) {
                deps.add(":res_release")
            }

            if (!includeManifest) {
                deps.add(":build_config_${depsOfFlavor}")
            } else {
                deps.add(":build_config")
            }

            for (Dependency dependency : finalDependenciesGraph.get(project).get("release")) {
                deps.add(dependency.srcCanonicalName())
                if (dependency.hasResPart() && !dependency.srcCanonicalName().equals(
                        dependency.resCanonicalName())) {
                    deps.add(dependency.resCanonicalName())
                } else if (dependency.hasMultipleResPart()) {
                    deps.addAll(dependency.multipleResCanonicalNames())
                }
            }

            for (String srcDir : ProjectHelper.getProjectSrcSet(project, "main")) {
                srcSet.add(srcDir + "/**/*.java")
            }
            for (String srcDir : ProjectHelper.getProjectSrcSet(project, "release")) {
                srcSet.add(srcDir + "/**/*.java")
            }
        } else {
            String resDir = ProjectHelper.getProjectResDir(project, "main")
            if (!StringUtil.isEmpty(resDir)) {
                deps.add(":res_main")
            }
            resDir = ProjectHelper.getProjectResDir(project, selfVariant)
            if (!StringUtil.isEmpty(resDir)) {
                deps.add(":res_${selfVariant}")
            }
            for (String srcDir : ProjectHelper.getProjectSrcSet(project, "main")) {
                srcSet.add(srcDir + "/**/*.java")
            }
            for (String srcDir : ProjectHelper.getProjectSrcSet(project, selfVariant)) {
                srcSet.add(srcDir + "/**/*.java")
            }
            if (!"main".equals(selfFlavor)) {
                resDir = ProjectHelper.getProjectResDir(project, selfFlavor)
                if (!StringUtil.isEmpty(resDir)) {
                    deps.add(":res_${selfFlavor}")
                }
                resDir = ProjectHelper.getProjectResDir(project,
                        selfFlavor + selfVariant.capitalize())
                if (!StringUtil.isEmpty(resDir)) {
                    deps.add(":res_${selfFlavor}_${selfVariant}")
                }
                for (String srcDir : ProjectHelper.getProjectSrcSet(project, selfFlavor)) {
                    srcSet.add(srcDir + "/**/*.java")
                }
                for (String srcDir : ProjectHelper.getProjectSrcSet(project,
                        selfFlavor + selfVariant.capitalize())) {
                    srcSet.add(srcDir + "/**/*.java")
                }
            }

            deps.add(":build_config_${depsOfFlavor}")

            if ("main".equals(selfFlavor)) {
                for (Dependency dependency :
                        finalDependenciesGraph.get(project).get(selfVariant)) {
                    deps.add(dependency.srcCanonicalName())
                    if (dependency.hasResPart() && !dependency.srcCanonicalName().equals(
                            dependency.resCanonicalName())) {
                        deps.add(dependency.resCanonicalName())
                    } else if (dependency.hasMultipleResPart()) {
                        deps.addAll(dependency.multipleResCanonicalNames())
                    }
                }
            } else {
                for (Dependency dependency :
                        finalDependenciesGraph.get(project).get(depsOfFlavor)) {
                    deps.add(dependency.srcCanonicalName())
                    if (dependency.hasResPart() && !dependency.srcCanonicalName().equals(
                            dependency.resCanonicalName())) {
                        deps.add(dependency.resCanonicalName())
                    } else if (dependency.hasMultipleResPart()) {
                        deps.addAll(dependency.multipleResCanonicalNames())
                    }
                }
            }
        }

        String jniLibsDir = ProjectHelper.getProjectJniLibsDir(project, "main")
        if (!StringUtil.isEmpty(jniLibsDir)) {
            deps.add(":native_libs")
        }

        List<String> annotationProcessorDeps = new ArrayList<>()
        annotationProcessorDeps.add("//" + mOkBuckDir.name +
                "/annotation_processor_deps" +
                ProjectHelper.getProjectPathDiff(mRootProject, project) +
                ":all_jars")
        annotationProcessorDeps.add("//" +
                mOkBuckDir.name + "/annotation_processor_deps" +
                ProjectHelper.getProjectPathDiff(mRootProject, project) +
                ":all_aars")

        // TODO manifest multi flavor support
        rules.add(new AndroidLibraryRule(ruleName, Arrays.asList("PUBLIC"), deps, srcSet,
                includeManifest ? ProjectHelper.getProjectManifestFile(project, "main") : null,
                mDependencyAnalyzer.annotationProcessors.get(project).asList(),
                annotationProcessorDeps, excludeAppClass))
    }

    private void addAndroidResRule(
            Project project, Map<Project, Map<String, Set<Dependency>>> finalDependenciesGraph,
            List<AbstractBuckRule> rules, String selfFlavor, String depsOfFlavor, String ruleName
    ) {
        String resDir = ProjectHelper.getProjectResDir(project, selfFlavor)
        if (!StringUtil.isEmpty(resDir)) {
            List<String> resDeps = new ArrayList<>()
            for (Dependency dependency : finalDependenciesGraph.get(project).get(depsOfFlavor)) {
                if (dependency.hasResPart()) {
                    resDeps.add(dependency.resCanonicalName())
                } else if (dependency.hasMultipleResPart()) {
                    resDeps.addAll(dependency.multipleResCanonicalNames())
                }
            }
            String assetsDir = ProjectHelper.getProjectAssetsDir(project, selfFlavor)
            rules.add(new AndroidResourceRule(ruleName, Arrays.asList("PUBLIC"), resDeps,
                    resDir, mResPackages.get(project.name), assetsDir))
        }
    }

    private void createAndroidAppRules(
            Project project, Map<Project, Map<String, Set<Dependency>>> finalDependenciesGraph,
            String appClassSource, List<Dependency> exopackageRuleDependencies, boolean exopackage,
            List<AbstractBuckRule> rules
    ) {
        if (exopackage) {
            createExopackageRules(project, appClassSource, exopackageRuleDependencies, rules)
        }
        createAndroidLibraryRules(project, finalDependenciesGraph, rules, false, exopackage)
        if (ProjectHelper.exportFlavor(project)) {
            addManifestRule(finalDependenciesGraph, project, rules, "main", "main")
            for (String flavor : ProjectHelper.getProductFlavors(project).keySet()) {
                List<String> binDeps = new ArrayList<>()
                binDeps.add(":src_${flavor}_debug")
                if (!StringUtil.isEmpty(ProjectHelper.getProjectResDir(project, "main"))) {
                    binDeps.add(":res_main")
                }
                if (!StringUtil.isEmpty(ProjectHelper.getProjectResDir(project, "debug"))) {
                    binDeps.add(":res_debug")
                }
                if (!StringUtil.isEmpty(ProjectHelper.getProjectResDir(project, flavor))) {
                    binDeps.add(":res_${flavor}")
                }
                if (!StringUtil.isEmpty(
                        ProjectHelper.getProjectResDir(project, "${flavor}Debug"))) {
                    binDeps.add(":res_${flavor}_debug")
                }
                if (exopackage) {
                    binDeps.add(":app_lib_${flavor}_debug")
                }
                if (ProjectHelper.getMultiDexEnabled(project)) {
                    rules.add(new AndroidBinaryRule("bin_${flavor}_debug", Arrays.asList("PUBLIC"),
                            binDeps, ":manifest",
                            "//${mKeystoreDir}${ProjectHelper.getProjectPathDiff(mRootProject, project)}:key_store",
                            mLinearAllocHardLimit, mPrimaryDexPatterns, exopackage))
                } else {
                    rules.add(new AndroidBinaryRule("bin_${flavor}_debug", Arrays.asList("PUBLIC"),
                            binDeps, ":manifest",
                            "//${mKeystoreDir}${ProjectHelper.getProjectPathDiff(mRootProject, project)}:key_store", exopackage))
                }

                binDeps = new ArrayList<>()
                binDeps.add(":src_${flavor}_release")
                if (!StringUtil.isEmpty(ProjectHelper.getProjectResDir(project, "main"))) {
                    binDeps.add(":res_main")
                }
                if (!StringUtil.isEmpty(ProjectHelper.getProjectResDir(project, "release"))) {
                    binDeps.add(":res_release")
                }
                if (!StringUtil.isEmpty(ProjectHelper.getProjectResDir(project, flavor))) {
                    binDeps.add(":res_${flavor}")
                }
                if (!StringUtil.isEmpty(
                        ProjectHelper.getProjectResDir(project, "${flavor}Release"))) {
                    binDeps.add(":res_${flavor}_release")
                }
                if (exopackage) {
                    binDeps.add(":app_lib_${flavor}_release")
                }
                if (ProjectHelper.getMultiDexEnabled(project)) {
                    rules.add(
                            new AndroidBinaryRule("bin_${flavor}_release", Arrays.asList("PUBLIC"),
                                    binDeps, ":manifest",
                                    "//${mKeystoreDir}${ProjectHelper.getProjectPathDiff(mRootProject, project)}:key_store",
                                    mLinearAllocHardLimit, mPrimaryDexPatterns, exopackage))
                } else {
                    rules.add(
                            new AndroidBinaryRule("bin_${flavor}_release", Arrays.asList("PUBLIC"),
                                    binDeps, ":manifest",
                                    "//${mKeystoreDir}${ProjectHelper.getProjectPathDiff(mRootProject, project)}:key_store", exopackage))
                }
            }
        } else {
            addManifestRule(finalDependenciesGraph, project, rules, "main", "main")
            List<String> binDeps = new ArrayList<>()
            binDeps.add(":src_debug")
            if (!StringUtil.isEmpty(ProjectHelper.getProjectResDir(project, "main"))) {
                binDeps.add(":res_main")
            }
            if (!StringUtil.isEmpty(ProjectHelper.getProjectResDir(project, "debug"))) {
                binDeps.add(":res_debug")
            }
            if (exopackage) {
                binDeps.add(":app_lib_debug")
            }
            if (ProjectHelper.getMultiDexEnabled(project)) {
                rules.add(new AndroidBinaryRule("bin_debug", Arrays.asList("PUBLIC"),
                        binDeps, ":manifest",
                        "//${mKeystoreDir}${ProjectHelper.getProjectPathDiff(mRootProject, project)}:key_store",
                        mLinearAllocHardLimit, mPrimaryDexPatterns, exopackage))
            } else {
                rules.add(new AndroidBinaryRule("bin_debug", Arrays.asList("PUBLIC"),
                        binDeps, ":manifest",
                        "//${mKeystoreDir}${ProjectHelper.getProjectPathDiff(mRootProject, project)}:key_store", exopackage))
            }

            binDeps = new ArrayList<>()
            binDeps.add(":src_release")
            if (!StringUtil.isEmpty(ProjectHelper.getProjectResDir(project, "main"))) {
                binDeps.add(":res_main")
            }
            if (!StringUtil.isEmpty(ProjectHelper.getProjectResDir(project, "debug"))) {
                binDeps.add(":res_release")
            }
            if (exopackage) {
                binDeps.add(":app_lib_release")
            }
            if (ProjectHelper.getMultiDexEnabled(project)) {
                rules.add(new AndroidBinaryRule("bin_release", Arrays.asList("PUBLIC"),
                        binDeps, ":manifest",
                        "//${mKeystoreDir}${ProjectHelper.getProjectPathDiff(mRootProject, project)}:key_store",
                        mLinearAllocHardLimit, mPrimaryDexPatterns, exopackage))
            } else {
                rules.add(new AndroidBinaryRule("bin_release", Arrays.asList("PUBLIC"),
                        binDeps, ":manifest",
                        "//${mKeystoreDir}${ProjectHelper.getProjectPathDiff(mRootProject, project)}:key_store", exopackage))
            }
        }
    }

    private void addManifestRule(
            Map<Project, Map<String, Set<Dependency>>> finalDependenciesGraph, Project project,
            List<AbstractBuckRule> rules, String selfFlavor, String depsOfFlavor
    ) {
        List<String> manifestDeps = new ArrayList<>()
        for (Dependency dependency : finalDependenciesGraph.get(project).get(depsOfFlavor)) {
            Project internalDep = ProjectHelper.getInternalDependencyProject(mRootProject,
                    dependency.depFile)
            if (internalDep != null) {
                // internal android lib/app module dependency
                switch (ProjectHelper.getSubProjectType(internalDep)) {
                    case ProjectHelper.ProjectType.AndroidAppProject:
                    case ProjectHelper.ProjectType.AndroidLibProject:
                        manifestDeps.add(dependency.srcCanonicalName())
                        break
                    case ProjectHelper.ProjectType.JavaLibProject:
                    default:
                        break
                }
            } else {
                if (dependency.hasResPart()) {
                    // aar dependencies
                    manifestDeps.add(dependency.srcCanonicalName())
                }
            }
        }

        rules.add(new BuckGenManifestRule("generate_manifest_main",
                ProjectHelper.getProjectManifestFile(project, selfFlavor), "AndroidManifest.xml",
                ProjectHelper.getVersionName(project, "default"),
                ProjectHelper.getVersionCode(project, "default"),
                ProjectHelper.getMinSdkVersion(project, "default"),
                ProjectHelper.getTargetSdkVersion(project, "default"), true))

        rules.add(new AndroidManifestRule(Arrays.asList("PUBLIC"), manifestDeps, ":generate_manifest_main"))
    }
}
