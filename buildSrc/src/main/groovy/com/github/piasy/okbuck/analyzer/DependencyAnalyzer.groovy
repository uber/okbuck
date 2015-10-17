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

package com.github.piasy.okbuck.analyzer

import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile

class DependencyAnalyzer {
    private final Project mRootProject
    private final String mBuildVariant
    private final Map<String, Set<File>> mAllSubProjectsExternalDependencies = new HashMap<>()
    private
    final Map<String, Set<File>> mAllSubProjectsExternalDependenciesExcluded = new HashMap<>()
    private final Map<String, Set<Project>> mAllSubProjectsInternalDependencies = new HashMap<>()
    private
    final Map<String, Set<Project>> mAllSubProjectsInternalDependenciesExcluded = new HashMap<>()
    private final Map<String, Set<File>> mAllSubProjectsAptDependencies = new HashMap<>()
    private final Map<String, Set<String>> mAnnotationProcessors = new HashMap<>()

    private final Map<String, Set<File>> mAllSubProjectsExternalTestDependencies = new HashMap<>()
    private
    final Map<String, Set<Project>> mAllSubProjectsInternalTestDependencies = new HashMap<>()

    public DependencyAnalyzer(Project rootProject, String variant) {
        mRootProject = rootProject
        mBuildVariant = variant

        analyse()
    }

    /**
     * analyse project dependencies, including `compile`, `apt`, `provided`, `testCompile`,
     * `debugCompile`, `releaseCompile`, dependencies including external (maven or local jar) and
     * internal (sub module, i.e. `compile project('prj')`).
     * */
    private void analyse() {
        // NOTE!!! below step calls' order matters

        // analyse compile dependencies
        analyseCompileDependencies()

        // analyse apt dependencies
        analyseAptDependencies()

        // extract annotation processors
        extractAnnotationProcessors()

        // exclude compile dependencies from apt dependencies
        // NOTE!!! These dependencies should not be excluded, i.e. if guava is a compile dependency
        // and guava is depended and used by an annotation processor, if excluded, guava will be
        // not available...
        //excludeCompileDependenciesFromApt()

        // exclude internal dependencies' external dependencies
        excludeInternalsExternalDependencies()
        excludeInternalsInternalDependencies()
    }

    public Map<String, Set<File>> getAllSubProjectsExternalDependencies() {
        return mAllSubProjectsExternalDependencies
    }

    public Map<String, Set<File>> getAllSubProjectsExternalDependenciesExcluded() {
        return mAllSubProjectsExternalDependenciesExcluded
    }

    public Map<String, Set<Project>> getAllSubProjectsInternalDependencies() {
        return mAllSubProjectsInternalDependencies
    }

    public Map<String, Set<Project>> getAllSubProjectsInternalDependenciesExcluded() {
        return mAllSubProjectsInternalDependenciesExcluded
    }

    public Map<String, Set<File>> getAllSubProjectsAptDependencies() {
        return mAllSubProjectsAptDependencies
    }

    public Map<String, Set<String>> getAnnotationProcessors() {
        return mAnnotationProcessors
    }

    private extractAnnotationProcessors() {
        mRootProject.subprojects { project ->
            mAnnotationProcessors.put(project.name, new HashSet<String>())
            for (File file : mAllSubProjectsAptDependencies.get(project.name)) {
                try {
                    JarFile jar = new JarFile(file)
                    for (JarEntry entry : jar.entries()) {
                        if (entry.name.equals(
                                "META-INF/services/javax.annotation.processing.Processor")) {
                            BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(jar.getInputStream(entry)))
                            String processor;
                            while ((processor = reader.readLine()) != null) {
                                mAnnotationProcessors.get(project.name).add(processor)
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

    private excludeInternalsExternalDependencies() {
        mRootProject.subprojects { project ->
            // for each sub project
            mAllSubProjectsExternalDependenciesExcluded.put(project.name,
                    new HashSet<>(mAllSubProjectsExternalDependencies.get(project.name)))
            for (Project projectDep : mAllSubProjectsInternalDependencies.get(project.name)) {
                // for each internal dependency of this sub project
                for (File mavenDep : mAllSubProjectsExternalDependencies.get(projectDep.name)) {
                    // iterate over the external dependencies of this **internal dependency**

                    // if the **internal dependency**'s external dependency is contained in the sub
                    // project's external dependencies, which means this exact external dependency
                    // is contained by the sub project's internal dependency
                    if (mAllSubProjectsExternalDependencies.get(project.name).contains(mavenDep) &&
                            mAllSubProjectsExternalDependenciesExcluded.get(project.name).
                                    contains(mavenDep)) {
                        // so exclude it
                        println "${project.name}'s compile dependency ${mavenDep.absolutePath} is contained in ${projectDep.name}, exclude it"
                        mAllSubProjectsExternalDependenciesExcluded.get(project.name).
                                remove(mavenDep)
                    }
                }
            }
        }
    }

    private excludeInternalsInternalDependencies() {
        mRootProject.subprojects { project ->
            // for each sub project
            mAllSubProjectsInternalDependenciesExcluded.put(project.name,
                    new HashSet<>(mAllSubProjectsInternalDependencies.get(project.name)))
            for (Project projectDep : mAllSubProjectsInternalDependencies.get(project.name)) {
                // for each internal dependency of this sub project
                for (Project dep : mAllSubProjectsInternalDependencies.get(projectDep.name)) {
                    // iterate over the internal dependencies of this **internal dependency**

                    // if the **internal dependency**'s internal dependency is contained in the sub
                    // project's internal dependencies, which means this exact internal dependency
                    // is contained by the sub project's internal dependency
                    if (mAllSubProjectsInternalDependencies.get(project.name).contains(dep) &&
                            mAllSubProjectsInternalDependenciesExcluded.get(project.name).
                                    contains(dep)) {
                        // so exclude it
                        println "${project.name}'s project dependency ${dep.name} is contained in ${projectDep.name}, exclude it"
                        mAllSubProjectsInternalDependenciesExcluded.get(project.name).remove(dep)
                    }
                }
            }
        }
    }

    private analyseAptDependencies() {
        mRootProject.subprojects { project ->
            mAllSubProjectsAptDependencies.put(project.name, new HashSet<File>())
            try {
                project.configurations.getByName("apt").resolve().each { dependency ->
                    mAllSubProjectsAptDependencies.get(project.name).add(dependency)
                }
            } catch (Exception e) {
                println "${project.name} doesn't contain apt configuration"
            }
            try {
                project.configurations.getByName("provided").resolve().each { dependency ->
                    mAllSubProjectsAptDependencies.get(project.name).add(dependency)
                }
            } catch (Exception e) {
                println "${project.name} doesn't contain provided configuration"
            }
            try {
                project.configurations.getByName("${mBuildVariant}Compile").
                        resolve().
                        each { dependency ->
                            mAllSubProjectsAptDependencies.get(project.name).add(dependency)
                        }
            } catch (Exception e) {
                println "${project.name} doesn't have ${mBuildVariant}Compile dependencies (from analyseAptDependencies)"
            }
        }
    }

    private analyseCompileDependencies() {
        mRootProject.subprojects { project ->
            // for each sub project
            mAllSubProjectsExternalDependencies.put(project.name, new HashSet<File>())
            mAllSubProjectsInternalDependencies.put(project.name, new HashSet<Project>())
            try {
                project.configurations.getByName("compile").resolve().each { dependency ->
                    // for each of its compile dependency, if dependency's path start with
                    // **another** sub project's build path, it's an internal dependency(project),
                    // otherwise, it's an external dependency, whether maven/m2/local jar/aar.
                    boolean isProjectDep = false
                    Project projectDep = null
                    for (Project subProject : mRootProject.subprojects) {
                        if (!project.projectDir.equals(
                                subProject.projectDir) && dependency.absolutePath.
                                startsWith(subProject.buildDir.absolutePath)) {
                            isProjectDep = true
                            projectDep = subProject
                            break
                        }
                    }
                    if (isProjectDep) {
                        println "${project.name}'s dependency ${dependency.absolutePath} is an internal dependency, sub project: ${projectDep}"
                        mAllSubProjectsInternalDependencies.get(project.name).add(projectDep)
                    } else {
                        println "${project.name}'s dependency ${dependency.absolutePath} is an external dependency"
                        mAllSubProjectsExternalDependencies.get(project.name).add(dependency)
                    }
                }
            } catch (Exception e) {
                println "${project.name} doesn't have compile dependencies"
            }

            try {
                project.configurations.getByName("${mBuildVariant}Compile").
                        resolve().
                        each { dependency ->
                            boolean isProjectDep = false
                            Project projectDep = null
                            for (Project subProject : mRootProject.subprojects) {
                                if (!project.projectDir.equals(
                                        subProject.projectDir) && dependency.absolutePath.
                                        startsWith(subProject.buildDir.absolutePath)) {
                                    isProjectDep = true
                                    projectDep = subProject
                                    break
                                }
                            }
                            if (isProjectDep) {
                                println "${project.name}'s ${mBuildVariant}Compile dependency ${dependency.absolutePath} is an internal dependency, sub project: ${projectDep}"
                                mAllSubProjectsInternalDependencies.get(project.name).
                                        add(projectDep)
                            } else {
                                println "${project.name}'s ${mBuildVariant}Compile dependency ${dependency.absolutePath} is an external dependency"
                                mAllSubProjectsExternalDependencies.get(project.name).
                                        add(dependency)
                            }
                        }
            } catch (Exception e) {
                println "${project.name} doesn't have ${mBuildVariant}Compile dependencies"
            }
        }
    }
}