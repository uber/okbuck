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

import com.github.piasy.okbuck.helper.ProjectHelper
import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile

public class DependencyAnalyzer {
    private final Project mRootProject
    private final String mBuildVariant
    private final File mOkBuckDir
    private final Map<Project, Set<File>> mDependenciesGraph = new HashMap<>()
    private final Map<File, Set<Project>> mDependerGraph = new HashMap<>()
    private final Map<Project, Set<Project>> mInternalDependencies = new HashMap<>()

    private final Map<Project, Set<Dependency>> mFinalDependenciesGraph = new HashMap<>()

    private final Map<Project, Set<File>> mAptDependencies = new HashMap<>()
    private final Map<Project, Set<String>> mAnnotationProcessors = new HashMap<>()

    private final Map<String, Set<File>> mExternalTestDependencies = new HashMap<>()
    private final Map<String, Set<Project>> mInternalTestDependencies = new HashMap<>()

    public DependencyAnalyzer(Project rootProject, String variant, File okBuckDir) {
        mRootProject = rootProject
        mBuildVariant = variant
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

        // unique sub project's dependency, only the dependencies that
        // **only depended by the sub project** should be contained in the unique dependencies
        analyseDepender()

        finalizeDependencies()
    }

    public Map<Project, Set<Dependency>> getFinalDependenciesGraph() {
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
            // for each sub project
            mDependenciesGraph.put(project, new HashSet<File>())
            mInternalDependencies.put(project, new HashSet<Project>())
            try {
                for (File dependency : project.configurations.getByName("compile").resolve()) {
                    mDependenciesGraph.get(project).add(dependency)
                    Project internalDep = Dependency.internalDependencyProject(mRootProject,
                            dependency)
                    if (internalDep != null) {
                        mInternalDependencies.get(project).add(internalDep)
                    }
                }
            } catch (Exception e) {
                println "${project.name} doesn't have compile dependencies"
            }

            try {
                for (File dependency :
                        project.configurations.getByName("${mBuildVariant}Compile").resolve()) {
                    mDependenciesGraph.get(project).add(dependency)
                    Project internalDep = Dependency.internalDependencyProject(mRootProject,
                            dependency)
                    if (internalDep != null) {
                        mInternalDependencies.get(project).add(internalDep)
                    }
                }
            } catch (Exception e) {
                println "${project.name} doesn't have ${mBuildVariant}Compile dependencies"
            }
        }
    }

    private analyseDepender() {
        for (Project project : mDependenciesGraph.keySet()) {
            for (File dependency : mDependenciesGraph.get(project)) {
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

    private void finalizeDependencies() {
        for (Project project : mDependenciesGraph.keySet()) {
            mFinalDependenciesGraph.put(project, new HashSet<Dependency>())
            for (File dependency : mDependenciesGraph.get(project)) {
                Dependency finalDependency
                if (mDependerGraph.get(dependency).size() == 1) {
                    // only one depender, this project is its root depender
                    finalDependency =
                            createFinalDependency(ProjectHelper.getPathDiff(mRootProject, project),
                                    dependency)
                } else {
                    // many depender, find the root one, or deps_common
                    finalDependency =
                            createFinalDependency(mDependerGraph.get(dependency), dependency)
                }
                mFinalDependenciesGraph.get(project).add(finalDependency)
            }
        }
    }

    private Dependency createFinalDependency(String dependerPathDiff, File dependency) {
        File dstDir = new File(mOkBuckDir.absolutePath + dependerPathDiff)

        Project internalDep = Dependency.internalDependencyProject(mRootProject, dependency)

        String srcName
        String resName = null
        if (internalDep == null) {
            // this dependency is an external one
            // TODO multiple os family support
            if (dependency.name.endsWith(".aar")) {
                srcName = "//${mOkBuckDir.name}${dependerPathDiff}:aar__${dependency.name}"
                resName = "//${mOkBuckDir.name}${dependerPathDiff}:aar__${dependency.name}"
            } else {
                srcName = "//${mOkBuckDir.name}${dependerPathDiff}:jar__${dependency.name}"
            }
        } else {
            // this dependency is an internal one
            String internalDepPathDiff = ProjectHelper.getPathDiff(mRootProject, internalDep)
            srcName = "/${internalDepPathDiff}:src"
            ProjectHelper.ProjectType type = ProjectHelper.getSubProjectType(internalDep)
            if (type == ProjectHelper.ProjectType.AndroidAppProject ||
                    type == ProjectHelper.ProjectType.AndroidLibProject) {
                // TODO custom project structure support
                File resDir = new File("${internalDep.projectDir.absolutePath}/src/main/res")
                if (resDir.exists()) {
                    resName = "/${internalDepPathDiff}:res"
                }
            }
        }
        return new Dependency(dependency, dstDir, srcName, resName)
    }

    private Dependency createFinalDependency(Set<Project> depender, File dependency) {
        Dependency finalDependency
        Project rootDepender = rootDepender(depender)
        if (rootDepender != null) {
            finalDependency =
                    createFinalDependency(ProjectHelper.getPathDiff(mRootProject, rootDepender),
                            dependency)
        } else {
            finalDependency = createFinalDependency(getDepsCommonPathDiff(depender), dependency)
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
                    ProjectHelper.getPathDiff(mRootProject, project).replace(File.separator, "_") +
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
                    if (!mInternalDependencies.get(anotherProject).contains(project)) {
                        isRoot = false
                        break
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