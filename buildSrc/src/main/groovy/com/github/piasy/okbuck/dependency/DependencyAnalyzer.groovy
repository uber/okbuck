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
import com.github.piasy.okbuck.helper.StringUtil
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.security.MessageDigest

public final class DependencyAnalyzer {
    private static Logger logger = Logging.getLogger(DependencyAnalyzer)
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private final Project mRootProject
    private final File mOkBuckDir
    private final boolean mCheckDepConflict
    private final DependencyExtractor mDependencyExtractor
    /**
     * this will be used for android_library, manifest rules
     * */
    private final Map<Project, Map<String, Set<Dependency>>> mFinalDependencies
    private final Map<Dependency, Set<Project>> mDependerMap
    private final MessageDigest mMD5

    public DependencyAnalyzer(
            Project rootProject, File okBuckDir, boolean checkDepConflict,
            DependencyExtractor dependencyExtractor
    ) {
        mRootProject = rootProject
        mOkBuckDir = okBuckDir
        mCheckDepConflict = checkDepConflict

        mDependencyExtractor = dependencyExtractor
        mFinalDependencies = new HashMap<>()
        mDependerMap = new HashMap<>()
        mMD5 = MessageDigest.getInstance("MD5")
    }

    public Map<Project, Map<String, Set<Dependency>>> getFinalDependencies() {
        return mFinalDependencies
    }

    private Map<Project, Map<String, Set<Dependency>>> getFullDependencies() {
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
        for (Project project : mRootProject.okbuck.toBuck) {
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
                    mFinalDependencies.get(project).put("main",
                            combineMain(project, fullDependencies.get(project)))
                    mFinalDependencies.get(project).put("release",
                            combineVariant(fullDependencies.get(project), "release"))

                    if (ProjectHelper.exportFlavor(project)) {
                        Map<String, ProductFlavor> flavorMap = ProjectHelper.getProductFlavors(
                                project)
                        for (String flavor : flavorMap.keySet()) {
                            mFinalDependencies.get(project).put(flavor,
                                    combineFlavor(fullDependencies.get(project), flavor))
                            mFinalDependencies.get(project).put((String) "${flavor}_debug",
                                    combineFlavorVariant(fullDependencies.get(project),
                                            flavor, "debug"))
                            mFinalDependencies.get(project).put((String) "${flavor}_release",
                                    combineFlavorVariant(fullDependencies.get(project),
                                            flavor, "release"))
                        }
                        mFinalDependencies.get(project).put("debug",
                                combineVariant(fullDependencies.get(project), "debug"))
                    }
                    break
                case ProjectHelper.ProjectType.AndroidAppProject:
                    mFinalDependencies.put(project, new HashMap<String, Set<Dependency>>())
                    mFinalDependencies.get(project).put("main",
                            combineMain(project, fullDependencies.get(project)))
                    mFinalDependencies.get(project).put("debug",
                            combineVariant(fullDependencies.get(project), "debug"))
                    mFinalDependencies.get(project).put("release",
                            combineVariant(fullDependencies.get(project), "release"))

                    if (ProjectHelper.exportFlavor(project)) {
                        Map<String, ProductFlavor> flavorMap = ProjectHelper.getProductFlavors(
                                project)
                        for (String flavor : flavorMap.keySet()) {
                            mFinalDependencies.get(project).put(flavor,
                                    combineFlavor(fullDependencies.get(project), flavor))
                            mFinalDependencies.get(project).put((String) "${flavor}_debug",
                                    combineFlavorVariant(fullDependencies.get(project),
                                            flavor, "debug"))
                            mFinalDependencies.get(project).put((String) "${flavor}_release",
                                    combineFlavorVariant(fullDependencies.get(project),
                                            flavor, "release"))
                        }
                    }
                    break
                default:
                    break
            }
        }
    }

    private static Set<Dependency> combineMain(
            Project project, Map<String, Set<Dependency>> origin
    ) {
        Set<Dependency> mainDeps = new HashSet<>()
        for (Dependency mainDep : origin.get("main")) {
            mainDeps.add(mainDep.defensiveCopy())
        }
        if (ProjectHelper.exportFlavor(project)) {
            Map<String, ProductFlavor> flavorMap = ProjectHelper.getProductFlavors(project)
            for (String flavor : flavorMap.keySet()) {
                for (Dependency flavorDep : origin.get(flavor)) {
                    addDepWhenUnique(mainDeps, flavorDep)
                }
            }
            for (Dependency variantDep : origin.get("debug")) {
                addDepWhenUnique(mainDeps, variantDep)
            }
        }
        for (Dependency variantDep : origin.get("release")) {
            addDepWhenUnique(mainDeps, variantDep)
        }
        return mainDeps
    }

    private static void addDepWhenUnique(Set<Dependency> mainDeps, Dependency dep) {
        if (mainDeps.contains(dep)) {
            return
        }
        for (Dependency dependency : mainDeps) {
            if (dependency.isDuplicate(dep)) {
                return
            }
        }
        mainDeps.add(dep)
    }

    private static Set<Dependency> combineVariant(
            Map<String, Set<Dependency>> origin, String variant
    ) {
        return combineFlavorVariant(origin, null, variant)
    }

    private static Set<Dependency> combineFlavor(
            Map<String, Set<Dependency>> origin, String flavor
    ) {
        return combineFlavorVariant(origin, flavor, null)
    }

    private static Set<Dependency> combineFlavorVariant(
            Map<String, Set<Dependency>> origin, String flavor, String variant
    ) {
        Set<Dependency> combined = new HashSet<>()

        for (Dependency mainDep : origin.get("main")) {
            combined.add(mainDep.defensiveCopy())
        }
        if (!StringUtil.isEmpty(flavor)) {
            for (Dependency flavorDep : origin.get(flavor)) {
                if (!combined.contains(flavorDep)) {
                    combined.add(flavorDep.defensiveCopy())
                }
            }
        }
        if (!StringUtil.isEmpty(variant)) {
            for (Dependency variantDep : origin.get(variant)) {
                if (!combined.contains(variantDep)) {
                    combined.add(variantDep.defensiveCopy())
                }
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
                            logger.warn "in ${project.name}, ${each.depFile.name} is duplicated with " +
                                    "${other.depFile.name}"
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
                            if (mDependerMap.containsKey(each)) {
                                mDependerMap.get(each).add(project)
                            } else {
                                Set<Project> projects = new HashSet<>()
                                projects.add(project)
                                mDependerMap.put(each, projects)
                            }
                            break
                        default:
                            break
                    }
                }
            }
        }

        for (Project project : mFinalDependencies.keySet()) {
            for (String flavorVariant : mFinalDependencies.get(project).keySet()) {
                for (Dependency each : mFinalDependencies.get(project).get(flavorVariant)) {
                    switch (each.type) {
                        case Dependency.DependencyType.LocalJarDependency:
                        case Dependency.DependencyType.LocalAarDependency:
                        case Dependency.DependencyType.MavenJarDependency:
                        case Dependency.DependencyType.MavenAarDependency:
                            each.setDstDir(new File("${mOkBuckDir.absolutePath}/" +
                                    getHashOfDependerSet(mDependerMap.get(each))))
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
                        each.setDstDir(new File("${mOkBuckDir.absolutePath}/" +
                                "${project.name}_apt_deps"))
                        break
                    default:
                        break
                }
            }
        }
    }

    private String getHashOfDependerSet(Set<Project> dependerSet) {
        String concatName = ""
        for (Project project : toSortedList(dependerSet)) {
            concatName += project.name + "_"
        }
        return bytesToHex(mMD5.digest(concatName.bytes))
    }

    private static List<Project> toSortedList(Set<Project> dependerSet) {
        List<Project> sorted = new ArrayList<>()
        for (Project project : dependerSet) {
            sorted.add(project)
        }
        Collections.sort(sorted, new Comparator<Project>() {
            @Override
            int compare(Project p1, Project p2) {
                return p1.name.compareTo(p2.name)
            }
        })
        return sorted
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
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
        logger.debug "############################ mFinalDependencies end ##############################"
    }
}