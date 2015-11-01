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

import java.util.jar.JarEntry
import java.util.jar.JarFile

public final class DependencyAnalyzer {

    private final Project mRootProject

    private final File mOkBuckDir

    private final Map<Project, Map<String, Set<File>>> mDependenciesGraph = new HashMap<>()

    private final Map<File, Set<Project>> mDependerGraph = new HashMap<>()

    private final Map<Project, Map<String, Set<Project>>> mInternalDependencies = new HashMap<>()

    private
    final Map<Project, Map<String, Set<Dependency>>> mFinalDependenciesGraph = new HashMap<>()

    private final Map<Project, Set<File>> mAptDependencies = new HashMap<>()

    private final Map<Project, Set<String>> mAnnotationProcessors = new HashMap<>()

    private final Map<String, Set<File>> mExternalTestDependencies = new HashMap<>()

    private final Map<String, Set<Project>> mInternalTestDependencies = new HashMap<>()

    public DependencyAnalyzer(Project rootProject, File okBuckDir) {
        mRootProject = rootProject
        mOkBuckDir = okBuckDir

        analyse()
    }

    /**
     * analyse project dependencies, including `compile`, `apt`, `provided`, `testCompile`,
     * `debugCompile`, `releaseCompile`, dependencies including external (maven or local jar) and
     * internal (sub module, i.e. `compile project('prj')`).
     * */
    private void analyse() {
        // NOTE!!! below step calls' order matters

        // analyse apt dependencies
        analyseAptDependencies()

        // extract annotation processors
        extractAnnotationProcessors()

        // analyse compile dependencies
        analyseCompileDependencies()
        printDependenciesGraph()

        // unique sub project's dependency, only the dependencies that
        // **only depended by the sub project** should be contained in the unique dependencies
        analyseDepender()

        finalizeDependencies()
    }

    public Map<Project, Map<String, Set<Dependency>>> getFinalDependenciesGraph() {
        return mFinalDependenciesGraph
    }

    public Map<Project, Set<File>> getAptDependencies() {
        return mAptDependencies
    }

    public Map<Project, Set<String>> getAnnotationProcessors() {
        return mAnnotationProcessors
    }

    private analyseAptDependencies() {
        for (Project project : mRootProject.subprojects) {
            if (ProjectHelper.getSubProjectType(project) == ProjectHelper.ProjectType.Unknown) {
                continue
            }
            mAptDependencies.put(project, new HashSet<File>())
            try {
                for (File dependency : project.configurations.getByName("apt").resolve()) {
                    mAptDependencies.get(project).add(dependency)
                }
            } catch (Exception e) {
                println "${project.name} doesn't contain apt configuration"
            }
            try {
                for (File dependency : project.configurations.getByName("provided").resolve()) {
                    mAptDependencies.get(project).add(dependency)
                }
            } catch (Exception e) {
                println "${project.name} doesn't contain provided configuration"
            }
        }
    }

    private extractAnnotationProcessors() {
        for (Project project : mRootProject.subprojects) {
            if (ProjectHelper.getSubProjectType(project) == ProjectHelper.ProjectType.Unknown) {
                continue
            }
            mAnnotationProcessors.put(project, new HashSet<String>())
            for (File file : mAptDependencies.get(project)) {
                try {
                    JarFile jar = new JarFile(file)
                    for (JarEntry entry : jar.entries()) {
                        if (entry.name.equals(
                                "META-INF/services/javax.annotation.processing.Processor")) {
                            BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(jar.getInputStream(entry)))
                            String processor;
                            while ((processor = reader.readLine()) != null) {
                                mAnnotationProcessors.get(project).add(processor)
                            }
                            reader.close()
                            break
                        }
                    }
                } catch (Exception e) {
                    println "extract processor from ${file.absolutePath} failed!"
                }
            }
        }
    }

    private analyseCompileDependencies() {
        for (Project project : mRootProject.subprojects) {
            ProjectHelper.ProjectType type = ProjectHelper.getSubProjectType(project)
            if (type == ProjectHelper.ProjectType.Unknown) {
                continue
            }
            // for each sub project
            mDependenciesGraph.put(project, new HashMap<String, Set<File>>())
            mInternalDependencies.put(project, new HashMap<String, Set<Project>>())
            try {
                mDependenciesGraph.get(project).put("main", new HashSet<File>())
                mInternalDependencies.get(project).put("main", new HashSet<Project>())
                for (File dependency : project.configurations.getByName("compile").resolve()) {
                    mDependenciesGraph.get(project).get("main").add(dependency)
                    Project internalDep = ProjectHelper.
                            getInternalDependencyProject(mRootProject, dependency)
                    if (internalDep != null) {
                        mInternalDependencies.get(project).get("main").add(internalDep)
                    }
                }
            } catch (Exception e) {
                println "${project.name} doesn't have compile dependencies"
            }

            if (type == ProjectHelper.ProjectType.JavaLibProject) {
                continue
            }
            try {
                mDependenciesGraph.get(project).put("debug", new HashSet<File>())
                mInternalDependencies.get(project).put("debug", new HashSet<Project>())
                for (File dependency : project.configurations.getByName("debugCompile").resolve()) {
                    mDependenciesGraph.get(project).get("debug").add(dependency)
                    Project internalDep = ProjectHelper.
                            getInternalDependencyProject(mRootProject, dependency)
                    if (internalDep != null) {
                        mInternalDependencies.get(project).get("debug").add(internalDep)
                    }
                }
            } catch (Exception e) {
                println "${project.name} doesn't have debugCompile dependencies"
            }

            try {
                mDependenciesGraph.get(project).put("release", new HashSet<File>())
                mInternalDependencies.get(project).put("release", new HashSet<Project>())
                for (File dependency :
                        project.configurations.getByName("releaseCompile").resolve()) {
                    mDependenciesGraph.get(project).get("release").add(dependency)
                    Project internalDep = ProjectHelper.
                            getInternalDependencyProject(mRootProject, dependency)
                    if (internalDep != null) {
                        mInternalDependencies.get(project).get("release").add(internalDep)
                    }
                }
            } catch (Exception e) {
                println "${project.name} doesn't have releaseCompile dependencies"
            }

            if (ProjectHelper.exportFlavor(project)) {
                Map<String, ProductFlavor> flavorMap = ProjectHelper.getProductFlavors(project)
                for (String flavor : flavorMap.keySet()) {
                    try {
                        mDependenciesGraph.get(project).put(flavor, new HashSet<File>())
                        mInternalDependencies.get(project).put(flavor, new HashSet<Project>())
                        for (File dependency :
                                project.configurations.getByName("${flavor}Compile").resolve()) {
                            mDependenciesGraph.get(project).get(flavor).add(dependency)
                            Project internalDep = ProjectHelper.
                                    getInternalDependencyProject(mRootProject, dependency)
                            if (internalDep != null) {
                                mInternalDependencies.get(project).get(flavor).add(internalDep)
                            }
                        }
                    } catch (Exception e) {
                        println "${project.name} doesn't have ${flavor}Compile dependencies"
                    }
                }
            }
        }
    }

    private void printDependenciesGraph() {
        for (Project project : mDependenciesGraph.keySet()) {
            println "<<<<< ${project.name}"
            for (String flavor : mDependenciesGraph.get(project).keySet()) {
                println "\t<<< ${flavor}"
                for (File dependency : mDependenciesGraph.get(project).get(flavor)) {
                    println "\t\t${dependency.absolutePath}"
                }
                println "\t>>>"
            }
            println ">>>>>"
        }
    }

    private analyseDepender() {
        for (Project project : mDependenciesGraph.keySet()) {
            for (String flavor : mDependenciesGraph.get(project).keySet()) {
                for (File dependency : mDependenciesGraph.get(project).get(flavor)) {
                    if (mDependerGraph.containsKey(dependency)) {
                        mDependerGraph.get(dependency).add(project)
                    } else {
                        Set<Project> dependerSet = new HashSet<>()
                        dependerSet.add(project)
                        mDependerGraph.put(dependency, dependerSet)
                    }
                }
            }
        }
    }

    private void finalizeDependencies() {
        for (Project project : mDependenciesGraph.keySet()) {
            mFinalDependenciesGraph.put(project, new HashMap<String, Set<Dependency>>())
            String exportedFlavor
            switch (ProjectHelper.getSubProjectType(project)) {
                case ProjectHelper.ProjectType.AndroidAppProject:
                    if (ProjectHelper.exportFlavor(project)) {
                        for (String flavor : ProjectHelper.getProductFlavors(project).keySet()) {
                            exportedFlavor = "${flavor}_debug"
                            mFinalDependenciesGraph.get(project).
                                    put(exportedFlavor, new HashSet<Dependency>())
                            addFinalDependencies4Flavor(project, exportedFlavor, "main")
                            addFinalDependencies4Flavor(project, exportedFlavor, flavor)
                            addFinalDependencies4Flavor(project, exportedFlavor, "debug")

                            exportedFlavor = "${flavor}_release"
                            mFinalDependenciesGraph.get(project).
                                    put(exportedFlavor, new HashSet<Dependency>())
                            addFinalDependencies4Flavor(project, exportedFlavor, "main")
                            addFinalDependencies4Flavor(project, exportedFlavor, flavor)
                            addFinalDependencies4Flavor(project, exportedFlavor, "release")
                        }
                    } else {
                        mFinalDependenciesGraph.get(project).put("debug", new HashSet<Dependency>())
                        addFinalDependencies4Flavor(project, "debug", "main")
                        addFinalDependencies4Flavor(project, "debug", "debug")

                        mFinalDependenciesGraph.get(project).
                                put("release", new HashSet<Dependency>())
                        addFinalDependencies4Flavor(project, "release", "main")
                        addFinalDependencies4Flavor(project, "release", "release")
                    }
                    break
                case ProjectHelper.ProjectType.AndroidLibProject:
                    if (ProjectHelper.exportFlavor(project)) {
                        for (String flavor : ProjectHelper.getProductFlavors(project).keySet()) {
                            exportedFlavor = "${flavor}_debug"
                            mFinalDependenciesGraph.get(project).
                                    put(exportedFlavor, new HashSet<Dependency>())
                            addFinalDependencies4Flavor(project, exportedFlavor, "main")
                            addFinalDependencies4Flavor(project, exportedFlavor, flavor)
                            addFinalDependencies4Flavor(project, exportedFlavor, "debug")

                            exportedFlavor = "${flavor}_release"
                            mFinalDependenciesGraph.get(project).
                                    put(exportedFlavor, new HashSet<Dependency>())
                            addFinalDependencies4Flavor(project, exportedFlavor, "main")
                            addFinalDependencies4Flavor(project, exportedFlavor, flavor)
                            addFinalDependencies4Flavor(project, exportedFlavor, "release")
                        }
                    } else {
                        mFinalDependenciesGraph.get(project).
                                put("release", new HashSet<Dependency>())
                        addFinalDependencies4Flavor(project, "release", "main")
                        addFinalDependencies4Flavor(project, "release", "release")
                    }
                    break
                case ProjectHelper.ProjectType.JavaLibProject:
                    mFinalDependenciesGraph.get(project).put("main", new HashSet<Dependency>())
                    addFinalDependencies4Flavor(project, "main", "main")
                    break
            }
        }
    }

    private void addFinalDependencies4Flavor(Project project, String exportedFlavor,
            String internalFlavor) {
        for (File dependency : mDependenciesGraph.get(project).get(internalFlavor)) {
            Dependency finalDependency
            if (mDependerGraph.get(dependency).size() == 1) {
                // only one depender, this project is its root depender
                finalDependency =
                        createFinalDependency(
                                ProjectHelper.getProjectPathDiff(mRootProject, project),
                                dependency, project)
            } else {
                // many depender, find the root one, or deps_common
                finalDependency =
                        createFinalDependency(mDependerGraph.get(dependency), dependency,
                                project)
            }
            mFinalDependenciesGraph.get(project).get(exportedFlavor).add(finalDependency)
        }
    }

    private Dependency createFinalDependency(String dependerPathDiff, File dependency,
            Project project) {
        File dstDir = new File(mOkBuckDir.absolutePath + dependerPathDiff)

        Project internalDep = ProjectHelper.
                getInternalDependencyProject(mRootProject, dependency)

        if (internalDep == null) {
            // this dependency is an external one
            // TODO multiple os family support
            if (ProjectHelper.isLocalExternalDependency(project, dependency)) {
                // local libs
                return new LocalDependency(dependency, dstDir,
                        "//${mOkBuckDir.name}${dependerPathDiff}:jar__${dependency.name}")
            } else {
                // remote dependency
                if (dependency.name.endsWith(".aar")) {
                    String srcName = "//${mOkBuckDir.name}${dependerPathDiff}:aar__${dependency.name}"
                    return new RemoteDependency(dependency, dstDir, srcName, srcName)
                } else {
                    return new RemoteDependency(dependency, dstDir,
                            "//${mOkBuckDir.name}${dependerPathDiff}:jar__${dependency.name}", null)
                }
            }
        } else {
            // this dependency is an internal one
            String internalDepPathDiff = ProjectHelper.getProjectPathDiff(mRootProject, internalDep)
            if (dependency.name.endsWith(".aar")) {
                // android library
                String[] names = dependency.name.substring(0, dependency.name.indexOf(".aar")).
                        split("-")
                if (names.length == 2) {
                    // no flavor, main + release, `src`, `res_main`, `res_release`
                    boolean mainResExists = internalDepHasResPart(internalDep, "main")
                    boolean releaseResExists = internalDepHasResPart(internalDep, "release")
                    List<String> multipleResCanonicalNames = null
                    if (mainResExists || releaseResExists) {
                        multipleResCanonicalNames = new ArrayList<>()
                        if (mainResExists) {
                            multipleResCanonicalNames.add("/${internalDepPathDiff}:res_main")
                        }
                        if (releaseResExists) {
                            multipleResCanonicalNames.add("/${internalDepPathDiff}:res_release")
                        }
                    }
                    return new ModuleDependency(dependency, internalDep, "/${internalDepPathDiff}:src",
                            multipleResCanonicalNames)
                } else if (names.length == 3) {
                    // has flavor, `src_flavor_variant`, `res_main`, `res_flavor`, `res_variant`, `res_flavor_variant`
                    boolean mainResExists = internalDepHasResPart(internalDep, "main")
                    boolean flavorResExists = internalDepHasResPart(internalDep, names[1])
                    boolean variantResExists = internalDepHasResPart(internalDep, names[2])
                    boolean flavorVariantResExists =
                            internalDepHasResPart(internalDep, names[1] + names[2].capitalize())
                    List<String> multipleResCanonicalNames = null
                    if (mainResExists || flavorResExists || variantResExists ||
                            flavorVariantResExists) {
                        multipleResCanonicalNames = new ArrayList<>()
                        if (mainResExists) {
                            multipleResCanonicalNames.add("/${internalDepPathDiff}:res_main")
                        }
                        if (flavorResExists) {
                            multipleResCanonicalNames.add("/${internalDepPathDiff}:res_${names[1]}")
                        }
                        if (variantResExists) {
                            multipleResCanonicalNames.add("/${internalDepPathDiff}:res_${names[2]}")
                        }
                        if (flavorVariantResExists) {
                            multipleResCanonicalNames.
                                    add("/${internalDepPathDiff}:res_${names[1]}_${names[2]}")
                        }
                    }
                    return new ModuleDependency(dependency, internalDep,
                            "/${internalDepPathDiff}:src_${names[1]}_${names[2]}",
                            multipleResCanonicalNames)
                } else {
                    throw new IllegalStateException(
                            "Android library has no flavor or variant decorate")
                }
            } else {
                // java library
                return new ModuleDependency(dependency, internalDep, "/${internalDepPathDiff}:src", null)
            }
        }
    }

    private static boolean internalDepHasResPart(Project internalDep, String flavorVariant) {
        return StringUtil.isEmpty(ProjectHelper.getProjectResDir(internalDep, flavorVariant))
    }

    private Dependency createFinalDependency(Set<Project> depender, File dependency,
            Project project) {
        Dependency finalDependency
        Project rootDepender = rootDepender(depender)
        if (rootDepender != null) {
            finalDependency = createFinalDependency(
                    ProjectHelper.getProjectPathDiff(mRootProject, rootDepender), dependency,
                    project)
        } else {
            finalDependency =
                    createFinalDependency(getDepsCommonPathDiff(depender), dependency, project)
        }
        return finalDependency
    }

    /**
     * the add order doesn't matter the traversal order, proved by
     * {@code OkBuckGradlePlugin.hashSetAddTraversalTest}
     * */
    private String getDepsCommonPathDiff(Set<Project> depender) {
        String pathDiff = "/common_deps/"
        for (Project project : depender) {
            pathDiff +=
                    ProjectHelper.getProjectPathDiff(mRootProject, project).
                            replace(File.separator, "_") +
                            "_"
        }
        return pathDiff
    }

    /**
     * get root depender, if no root, return null, O(n ^ 2)
     * */
    private Project rootDepender(Set<Project> depender) {
        for (Project project : depender) {
            boolean isRoot = true
            for (Project anotherProject : depender) {
                if (project != anotherProject) {
                    for (String flavor : mInternalDependencies.get(anotherProject).keySet()) {
                        if (!mInternalDependencies.get(anotherProject).get(flavor).
                                contains(project)) {
                            isRoot = false
                            break
                        }
                    }
                }
            }
            if (isRoot) {
                return project
            }
        }

        return null
    }
}