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

import com.android.build.gradle.internal.dsl.ProductFlavor
import com.github.piasy.okbuck.composer.*
import com.github.piasy.okbuck.configs.BUCKFile
import com.github.piasy.okbuck.dependency.Dependency
import com.github.piasy.okbuck.dependency.DependencyAnalyzer
import com.github.piasy.okbuck.helper.ProjectHelper
import com.github.piasy.okbuck.helper.StringUtil
import com.github.piasy.okbuck.rules.AndroidResourceRule
import com.github.piasy.okbuck.rules.AppClassSourceRule
import com.github.piasy.okbuck.rules.GenAidlRule
import com.github.piasy.okbuck.rules.PrebuiltNativeLibraryRule
import com.github.piasy.okbuck.rules.base.AbstractBuckRule
import org.gradle.api.Project

import static com.github.piasy.okbuck.helper.CheckUtil.checkStringNotEmpty

/**
 * Created by Piasy{github.com/Piasy} on 15/10/6.
 *
 * used to generate BUCK file content.
 */
public final class BuckFileGenerator {
    private final Project mRootProject
    private final DependencyAnalyzer mDependencyAnalyzer
    private final File mOkBuckDir
    private final Map<String, String> mResPackages
    private final Map<String, Integer> mLinearAllocHardLimit
    private final Map<String, List<String>> mPrimaryDexPatterns
    private final Map<String, Boolean> mExopackage
    private final Map<String, String> mAppClassSource
    private final Map<String, List<String>> mAppLibDependencies
    private final Map<String, List<String>> mFlavorFilter
    private final Map<String, List<String>> mCpuFilters

    public BuckFileGenerator(
            Project rootProject, DependencyAnalyzer dependencyAnalyzer, File okBuckDir,
            Map<String, String> resPackages, Map<String, Integer> linearAllocHardLimit,
            Map<String, List<String>> primaryDexPatterns, Map<String, Boolean> exopackage,
            Map<String, String> appClassSource, Map<String, List<String>> appLibDependencies,
            Map<String, List<String>> flavorFilter, Map<String, List<String>> cpuFilters
    ) {
        mRootProject = rootProject
        mDependencyAnalyzer = dependencyAnalyzer
        mOkBuckDir = okBuckDir
        mResPackages = resPackages
        mLinearAllocHardLimit = linearAllocHardLimit
        mPrimaryDexPatterns = primaryDexPatterns
        mExopackage = exopackage
        mAppClassSource = appClassSource
        mAppLibDependencies = appLibDependencies
        mFlavorFilter = flavorFilter
        mCpuFilters = cpuFilters
    }

    /**
     * generate {@code BUCKFile}
     */

    public Map<Project, BUCKFile> generate() {
        Map<Project, BUCKFile> buckFileMap = new HashMap<>()

        for (Project project : mRootProject.subprojects) {
            List<AbstractBuckRule> rules = new ArrayList<>()
            switch (ProjectHelper.getSubProjectType(project)) {
                case ProjectHelper.ProjectType.AndroidAppProject:
                    boolean exopackage = !mExopackage.containsKey(project.name) ? false :
                            mExopackage.get(project.name)
                    List<String> primaryDexPatterns = mPrimaryDexPatterns.get(project.name)
                    String appClassSource = mAppClassSource.get(project.name)
                    List<String> appLibDependencies = mAppLibDependencies.get(project.name)
                    List<String> cpuFilters = mCpuFilters.get(project.name)
                    if (exopackage && (primaryDexPatterns == null ||
                            StringUtil.isEmpty(appClassSource) || appLibDependencies == null)) {
                        throw new IllegalArgumentException("Please set primaryDexPatterns, " +
                                "appClassSource and appLibDependencies in your root build.gradle file")
                    }
                    Integer linearAllocHardLimit = mLinearAllocHardLimit.get(project.name)
                    if (linearAllocHardLimit == null) {
                        linearAllocHardLimit = 65535
                    }
                    Map<String, Set<Dependency>> finalDependencies =
                            mDependencyAnalyzer.finalDependencies.get(project)
                    createAndroidAppRules(rules, project, finalDependencies, appClassSource,
                            matchAppLibDependencies(appLibDependencies, finalDependencies),
                            exopackage, linearAllocHardLimit, primaryDexPatterns, cpuFilters)
                    break
                case ProjectHelper.ProjectType.AndroidLibProject:
                    createAndroidLibraryRules(rules, project,
                            mDependencyAnalyzer.finalDependencies.get(project), true, false)
                    break
                case ProjectHelper.ProjectType.JavaLibProject:
                    createJavaLibraryRules(rules, project,
                            mDependencyAnalyzer.finalDependencies.get(project))
                    break
                case ProjectHelper.ProjectType.Unknown:
                default:
                    break
            }

            if (!rules.empty) {
                buckFileMap.put(project, new BUCKFile(rules))
            } // else, `:libraries:common` will create a sub project `:libraries`, skip it
        }

        return buckFileMap
    }

    private Map<String, ProductFlavor> getFilteredFlavors(Project project) {
        Map<String, ProductFlavor> flavorMap = ProjectHelper.getProductFlavors(project)
        List<String> filter = mFlavorFilter.get(project.name)
        if (filter == null || filter.empty) {
            return flavorMap
        } else {
            Map<String, ProductFlavor> filtered = new HashMap<>()
            for (String flavor : filter) {
                if (flavorMap.containsKey(flavor)) {
                    filtered.put(flavor, flavorMap.get(flavor))
                } else {
                    throw new IllegalArgumentException("`${project.name}` doesn't have flavor " +
                            "named `${flavor}`, please correct your root project build.gradle file")
                }
            }
            return filtered
        }
    }

    private static List<Dependency> matchAppLibDependencies(
            List<String> deps, Map<String, Set<Dependency>> finalDependencies
    ) {
        List<Dependency> dependencies = new ArrayList<>()
        for (String dep : deps) {
            boolean found = false
            for (String flavor : finalDependencies.keySet()) {
                if (found) {
                    break
                }
                for (Dependency dependency : finalDependencies.get(flavor)) {
                    if (dependency.depFile.name.startsWith(dep)) {
                        dependencies.add(dependency)
                        found = true
                        // not break, because name inside deps are not unique, so maybe
                        // other deps match its name.
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
            List<AbstractBuckRule> rules, Project project,
            Map<String, Set<Dependency>> finalDependencies
    ) {
        rules.add(JavaLibraryRuleComposer.compose(project, mOkBuckDir,
                finalDependencies.get("main"),
                mDependencyAnalyzer.annotationProcessors.get(project)))

        rules.add(ProjectConfigRuleComposer.compose(project))
    }

    private void createExopackageRules(
            List<AbstractBuckRule> rules, Project project, String appClassSource,
            List<Dependency> exopackageRuleDependencies
    ) {
        rules.add(new AppClassSourceRule(appClassSource))
        if (ProjectHelper.exportFlavor(project)) {
            for (String flavor : getFilteredFlavors(project).keySet()) {
                rules.add(ExopackageAndroidLibraryRuleComposer.compose(exopackageRuleDependencies,
                        flavor, "debug"))
                rules.add(ExopackageAndroidLibraryRuleComposer.compose(exopackageRuleDependencies,
                        flavor, "release"))
            }
        } else {
            rules.add(ExopackageAndroidLibraryRuleComposer.composeWithoutFlavor(
                    exopackageRuleDependencies, "debug"))
            rules.add(ExopackageAndroidLibraryRuleComposer.composeWithoutFlavor(
                    exopackageRuleDependencies, "release"))
        }
    }

    private void createAndroidLibraryRules(
            List<AbstractBuckRule> rules, Project project,
            Map<String, Set<Dependency>> finalDependencies, boolean isForLibraryModule,
            boolean excludeAppClass
    ) {
        checkStringNotEmpty(mResPackages.get(project.name), "resPackage key-value pair must be " +
                "set for sub project ${project.name} in your root project build.gradle file");

        GenAidlRule genAidlRule = GenAidlRuleComposer.compose(project)
        String aidlRuleName = null
        if (genAidlRule != null) {
            rules.add(genAidlRule)
            aidlRuleName = genAidlRule.ruleName
        }

        if (ProjectHelper.exportFlavor(project)) {
            addAndroidResRuleIfNeed("res_main", project, "main", finalDependencies.get("main"),
                    rules)
            for (String flavor : getFilteredFlavors(project).keySet()) {
                addAndroidResRuleIfNeed("res_${flavor}", project, flavor,
                        finalDependencies.get(flavor), rules)
                addAndroidResRuleIfNeed("res_debug", project, "debug",
                        finalDependencies.get("debug"), rules)
                addAndroidResRuleIfNeed("res_release", project, "release",
                        finalDependencies.get("release"), rules)
                addAndroidResRuleIfNeed("res_${flavor}_debug", project, "${flavor}Debug",
                        finalDependencies.get((String) "${flavor}_debug"), rules)
                addAndroidResRuleIfNeed("res_${flavor}_release", project, "${flavor}Release",
                        finalDependencies.get((String) "${flavor}_release"), rules)

                rules.add(AndroidBuildConfigRuleComposer.compose("build_config_${flavor}_debug",
                        project, flavor, "debug", mResPackages.get(project.name)))
                rules.add(AndroidBuildConfigRuleComposer.compose("build_config_${flavor}_release",
                        project, flavor, "release", mResPackages.get(project.name)))

                rules.add(AndroidLibraryRuleComposer.composeWithFlavor(
                        "src_${flavor}_debug", project, mOkBuckDir,
                        finalDependencies.get((String) "${flavor}_debug"),
                        mDependencyAnalyzer.annotationProcessors.get(project), flavor, "debug",
                        isForLibraryModule, excludeAppClass, aidlRuleName))
                rules.add(AndroidLibraryRuleComposer.composeWithFlavor(
                        "src_${flavor}_release", project, mOkBuckDir,
                        finalDependencies.get((String) "${flavor}_release"),
                        mDependencyAnalyzer.annotationProcessors.get(project), flavor, "release",
                        isForLibraryModule, excludeAppClass, aidlRuleName))
            }
        } else {
            if (isForLibraryModule) {
                addAndroidResRuleIfNeed("res_main", project, "main",
                        finalDependencies.get("main"), rules)
                addAndroidResRuleIfNeed("res_release", project, "release",
                        finalDependencies.get("release"), rules)

                rules.add(AndroidBuildConfigRuleComposer.compose("build_config",
                        project, "default", "release", mResPackages.get(project.name)))

                rules.add(AndroidLibraryRuleComposer.compose4LibraryWithoutFlavor(
                        "src", project, mOkBuckDir, finalDependencies.get("release"),
                        mDependencyAnalyzer.annotationProcessors.get(project), isForLibraryModule,
                        excludeAppClass, aidlRuleName))
            } else {
                addAndroidResRuleIfNeed("res_main", project, "main",
                        finalDependencies.get("main"), rules)
                addAndroidResRuleIfNeed("res_debug", project, "debug",
                        finalDependencies.get("debug"), rules)
                addAndroidResRuleIfNeed("res_release", project, "release",
                        finalDependencies.get("release"), rules)

                rules.add(AndroidBuildConfigRuleComposer.compose("build_config_debug",
                        project, "default", "debug", mResPackages.get(project.name)))
                rules.add(AndroidBuildConfigRuleComposer.compose("build_config_release",
                        project, "default", "release", mResPackages.get(project.name)))

                rules.add(AndroidLibraryRuleComposer.compose4AppWithoutFlavor(
                        "src_debug", project, mOkBuckDir, finalDependencies.get("debug"),
                        mDependencyAnalyzer.annotationProcessors.get(project), "debug",
                        isForLibraryModule, excludeAppClass, aidlRuleName))
                rules.add(AndroidLibraryRuleComposer.compose4AppWithoutFlavor(
                        "src_release", project, mOkBuckDir, finalDependencies.get("release"),
                        mDependencyAnalyzer.annotationProcessors.get(project), "release",
                        isForLibraryModule, excludeAppClass, aidlRuleName))
            }
        }

        PrebuiltNativeLibraryRule rule = PreBuiltNativeLibraryRuleComposer.compose(project, "main")
        if (rule != null) {
            rules.add(rule)
        }
        rules.add(ProjectConfigRuleComposer.compose(project))
    }

    private void addAndroidResRuleIfNeed(
            String ruleName, Project project, String resRootDirName, Set<Dependency> dependencies,
            List<AbstractBuckRule> rules
    ) {
        AndroidResourceRule rule = AndroidResourceRuleComposer.compose(ruleName, project,
                resRootDirName, dependencies, mResPackages.get(project.name))
        if (rule != null) {
            rules.add(rule)
        }
    }

    private void createAndroidAppRules(
            List<AbstractBuckRule> rules, Project project,
            Map<String, Set<Dependency>> finalDependencies, String appClassSource,
            List<Dependency> exopackageRuleDependencies, boolean exopackage,
            int linearAllocHardLimit, List<String> primaryDexPatterns, List<String> cpuFilters
    ) {
        if (exopackage) {
            createExopackageRules(rules, project, appClassSource, exopackageRuleDependencies)
        }
        createAndroidLibraryRules(rules, project, finalDependencies, false, exopackage)
        rules.add(BuckGenManifestRuleComposer.compose(project))
        rules.add(AndroidManifestRuleComposer.compose(project, finalDependencies.get("main")))

        if (ProjectHelper.exportFlavor(project)) {
            for (String flavor : getFilteredFlavors(project).keySet()) {
                rules.add(AndroidBinaryRuleComposer.compose(project, flavor, "debug", exopackage,
                        linearAllocHardLimit, primaryDexPatterns, cpuFilters))
                rules.add(AndroidBinaryRuleComposer.compose(project, flavor, "release", exopackage,
                        linearAllocHardLimit, primaryDexPatterns, cpuFilters))
            }
        } else {
            rules.add(AndroidBinaryRuleComposer.composeWithoutFlavor(project, "debug", exopackage,
                    linearAllocHardLimit, primaryDexPatterns, cpuFilters))
            rules.add(AndroidBinaryRuleComposer.composeWithoutFlavor(project, "release", exopackage,
                    linearAllocHardLimit, primaryDexPatterns, cpuFilters))
        }
    }
}