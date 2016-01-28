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

package com.github.piasy.okbuck.dependency

import com.android.build.gradle.internal.dsl.ProductFlavor
import com.github.piasy.okbuck.helper.ProjectHelper
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

public final class DependencyAnalyzer {

    private static Logger logger = Logging.getLogger(DependencyAnalyzer)

    private final Project mRootProject

    private final File mOkBuckDir

    private final boolean mCheckDepConflict

    private final DependencyExtractor mDependencyExtractor

    /**
     * this will be used for android_library, manifest rules
     * */
    private final Map<Project, Map<String, Set<Dependency>>> mFinalDependencies

    public DependencyAnalyzer(
            Project rootProject, File okBuckDir, boolean checkDepConflict,
            DependencyExtractor dependencyExtractor
    ) {
        mRootProject = rootProject
        mOkBuckDir = okBuckDir
        mCheckDepConflict = checkDepConflict

        mDependencyExtractor = dependencyExtractor
        mFinalDependencies = new HashMap<>()
    }

    public Map<Project, Map<String, Set<Dependency>>> getFinalDependencies() {
        return mFinalDependencies
    }

    public Map<Project, Map<String, Set<Dependency>>> getFullDependencies() {
        return mDependencyExtractor.fullDependencies
    }

    public Map<Project, Set<Dependency>> getAptDependencies() {
        return mDependencyExtractor.aptDependencies
    }

    public Map<Project, Set<String>> getAnnotationProcessors() {
        return mDependencyExtractor.annotationProcessors
    }

    public void analyse() {
        mDependencyExtractor.extract()

        combineFlavorVariant()

        if (mCheckDepConflict) {
            checkDependencyDiffersByVersion()
        }

        allocateDstDir()
        printDependenciesGraph()
    }

    private void combineFlavorVariant() {
        for (Project project : mRootProject.subprojects) {
            switch (ProjectHelper.getSubProjectType(project)) {
                case ProjectHelper.ProjectType.JavaLibProject:
                    mFinalDependencies.put(project, new HashMap<String, Set<Dependency>>())
                    Set<Dependency> mainDeps = new HashSet<>()
                    for (Dependency mainDep : fullDependencies.get(project).get("main")) {
                        mainDeps.add(mainDep.defensiveCopy())
                    }
                    mFinalDependencies.get(project).put("main", mainDeps)
                    break
                case ProjectHelper.ProjectType.AndroidLibProject:
                    mFinalDependencies.put(project, new HashMap<String, Set<Dependency>>())
                    if (ProjectHelper.exportFlavor(project)) {
                        Map<String, ProductFlavor> flavorMap = ProjectHelper.getProductFlavors(
                                project)
                        for (String flavor : flavorMap.keySet()) {
                            mFinalDependencies.get(project).put((String) "${flavor}_debug",
                                    combineFlavorVariant(fullDependencies.get(project),
                                            flavor, "debug"))
                            mFinalDependencies.get(project).put((String) "${flavor}_release",
                                    combineFlavorVariant(fullDependencies.get(project),
                                            flavor, "release"))
                        }
                    } else {
                        mFinalDependencies.get(project).put("main_release",
                                combineVariant(fullDependencies.get(project), "release"))
                    }
                    break
                case ProjectHelper.ProjectType.AndroidAppProject:
                    mFinalDependencies.put(project, new HashMap<String, Set<Dependency>>())
                    if (ProjectHelper.exportFlavor(project)) {
                        Map<String, ProductFlavor> flavorMap = ProjectHelper.getProductFlavors(
                                project)
                        for (String flavor : flavorMap.keySet()) {
                            mFinalDependencies.get(project).put((String) "${flavor}_debug",
                                    combineFlavorVariant(fullDependencies.get(project),
                                            flavor, "debug"))
                            mFinalDependencies.get(project).put((String) "${flavor}_release",
                                    combineFlavorVariant(fullDependencies.get(project),
                                            flavor, "release"))
                        }
                    } else {
                        mFinalDependencies.get(project).put("main_debug",
                                combineVariant(fullDependencies.get(project), "debug"))
                        mFinalDependencies.get(project).put("main_release",
                                combineVariant(fullDependencies.get(project), "release"))
                    }
                    break
                default:
                    break
            }
        }
    }

    private static Set<Dependency> combineVariant(
            Map<String, Set<Dependency>> origin, String variant
    ) {
        Set<Dependency> combined = new HashSet<>()
        Set<Dependency> mainDeps = origin.get("main")
        Set<Dependency> variantDeps = origin.get(variant)
        combined.addAll(mainDeps)
        for (Dependency variantDep : variantDeps) {
            if (!combined.contains(variantDep)) {
                combined.add(variantDep)
            }
        }

        return combined
    }

    private static Set<Dependency> combineFlavorVariant(
            Map<String, Set<Dependency>> origin, String flavor, String variant
    ) {
        Set<Dependency> combined = new HashSet<>()
        Set<Dependency> mainDeps = origin.get("main")
        Set<Dependency> flavorDeps = origin.get(flavor)
        Set<Dependency> variantDeps = origin.get(variant)
        for (Dependency mainDep : mainDeps) {
            combined.add(mainDep.defensiveCopy())
        }
        for (Dependency variantDep : variantDeps) {
            if (!combined.contains(variantDep)) {
                combined.add(variantDep.defensiveCopy())
            }
        }
        for (Dependency flavorDep : flavorDeps) {
            if (!combined.contains(flavorDep)) {
                combined.add(flavorDep.defensiveCopy())
            }
        }

        return combined
    }

    private void checkDependencyDiffersByVersion() {
        for (Project project : mFinalDependencies.keySet()) {
            for (String flavorVariant : mFinalDependencies.get(project).keySet()) {
                for (Dependency each : mFinalDependencies.get(project).get(flavorVariant)) {
                    for (Dependency other : mFinalDependencies.get(project).get(flavorVariant)) {
                        if (!each.equals(other) && each.isDuplicate(other)) {
                            logger.warn "${each.getDepFile().absolutePath} is duplicated with " +
                                    "${other.getDepFile().absolutePath}"
                        }
                    }
                }
            }
        }
    }

    private void allocateDstDir() {
        for (Project project : mFinalDependencies.keySet()) {
            for (String flavorVariant : mFinalDependencies.get(project).keySet()) {
                for (Dependency each : mFinalDependencies.get(project).get(flavorVariant)) {
                    switch (each.type) {
                        case Dependency.DependencyType.LocalJarDependency:
                        case Dependency.DependencyType.LocalAarDependency:
                        case Dependency.DependencyType.MavenJarDependency:
                        case Dependency.DependencyType.MavenAarDependency:
                            each.setDstDir(new File(mOkBuckDir.absolutePath + File.separator +
                                    project.name + "_${flavorVariant}"))
                            break
                        default:
                            break
                    }
                }
            }
        }

        for (Project project : fullDependencies.keySet()) {
            for (String flavor : fullDependencies.get(project).keySet()) {
                for (Dependency each : fullDependencies.get(project).get(flavor)) {
                    switch (each.type) {
                        case Dependency.DependencyType.LocalJarDependency:
                        case Dependency.DependencyType.LocalAarDependency:
                        case Dependency.DependencyType.MavenJarDependency:
                        case Dependency.DependencyType.MavenAarDependency:
                            each.setDstDir(new File(mOkBuckDir.absolutePath + File.separator +
                                    project.name + "_${flavor}"))
                            break
                        default:
                            break
                    }
                }
            }
        }

        for (Project project : aptDependencies.keySet()) {
            for (Dependency each : aptDependencies.get(project)) {
                switch (each.type) {
                    case Dependency.DependencyType.LocalJarDependency:
                    case Dependency.DependencyType.LocalAarDependency:
                    case Dependency.DependencyType.MavenJarDependency:
                    case Dependency.DependencyType.MavenAarDependency:
                        each.setDstDir(new File(mOkBuckDir.absolutePath + File.separator +
                                project.name + "_apt_deps"))
                        break
                    default:
                        break
                }
            }
        }
    }

    private void printDependenciesGraph() {
        logger.debug "############################ mFinalDependencies start ############################"
        for (Project project : mFinalDependencies.keySet()) {
            logger.debug "<<<<< ${project.name}"
            for (String flavor : mFinalDependencies.get(project).keySet()) {
                logger.debug "\t<<< ${flavor}"
                for (Dependency dependency : mFinalDependencies.get(project).get(flavor)) {
                    logger.debug "\t\t${dependency.toString()} @@ ${dependency.srcCanonicalName}"
                }
                logger.debug "\t>>>"
            }
            logger.debug ">>>>>"
        }
        logger.debug "############################ mFinalDependencies start ############################"
    }
}