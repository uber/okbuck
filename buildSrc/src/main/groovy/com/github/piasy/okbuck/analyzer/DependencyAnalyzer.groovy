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
import org.gradle.api.artifacts.UnknownConfigurationException

import java.util.jar.JarEntry
import java.util.jar.JarFile

class DependencyAnalyzer {
    private final Project mRootProject
    private final String mBuildVariant
    private final Map<String, Set<File>> mAllSubProjectsExternalDependencies = new HashMap<>()
    private final Map<String, Set<String>> mAllSubProjectsInternalDependencies = new HashMap<>()
    private final Map<String, Set<File>> mAllSubProjectsAptDependencies = new HashMap<>()
    private final Map<String, Set<String>> mAnnotationProcessors = new HashMap<>()

    private final Map<String, Set<File>> mAllSubProjectsExternalTestDependencies = new HashMap<>()
    private final Map<String, Set<String>> mAllSubProjectsInternalTestDependencies = new HashMap<>()

    public DependencyAnalyzer(Project rootProject, String variant) {
        mRootProject = rootProject
        mBuildVariant = variant
    }

    /**
     * analyse project dependencies, including `compile`, `apt`, `provided`, `testCompile`,
     * `debugCompile`, `releaseCompile`, dependencies including external (maven or local jar) and
     * internal (sub module, i.e. `compile project('prj')`).
     * */
    public void analyse() {
        // NOTE!!! below step calls' order matters

        // analyse compile dependencies
        analyseCompileDependencies()

        // analyse apt dependencies
        analyseAptDependencies()

        // extract annotation processors
        extractAnnotationProcessors()

        // exclude compile dependencies from apt dependencies
        excludeCompileDependenciesFromApt()

        // exclude internal dependencies' external dependencies
        excludeInternalsExternalDependencies()
    }

    public Map<String, Set<File>> getAllSubProjectsExternalDependencies() {
        return mAllSubProjectsExternalDependencies
    }

    public Map<String, Set<String>> getAllSubProjectsInternalDependencies() {
        return mAllSubProjectsInternalDependencies
    }

    public Map<String, Set<File>> getAllSubProjectsAptDependencies() {
        return mAllSubProjectsAptDependencies
    }

    public Map<String, Set<String>> getAnnotationProcessors() {
        return mAnnotationProcessors
    }

    private excludeCompileDependenciesFromApt() {
        mRootProject.subprojects { project ->
            Set<File> excluded = new HashSet<>()
            for (File dependency : mAllSubProjectsAptDependencies.get(project.name)) {
                if (mAllSubProjectsExternalDependencies.get(project.name).contains(dependency)) {
                    println "${project.name}'s apt dependency ${dependency.absolutePath} is contained in compile dependencies, exclude it"
                    excluded.add(dependency)
                }
            }
            mAllSubProjectsAptDependencies.get(project.name).removeAll(excluded)
        }
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
            for (String projectDep : mAllSubProjectsInternalDependencies.get(project.name)) {
                // for each internal dependency of this sub project
                for (File mavenDep : mAllSubProjectsExternalDependencies.get(projectDep)) {
                    // iterate over the external dependencies of this **internal dependency**

                    // if the **internal dependency**'s external dependency is contained in the sub
                    // project's external dependencies, which means this exact external dependency
                    // is contained by the sub project's internal dependency
                    if (mAllSubProjectsExternalDependencies.get(project.name).contains(mavenDep)) {
                        // so exclude it
                        println "${project.name}'s compile dependency ${mavenDep.absolutePath} is contained in ${projectDep}, exclude it"
                        mAllSubProjectsExternalDependencies.get(project.name).remove(mavenDep)
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
            } catch (UnknownConfigurationException e) {
                println "${project.name} doesn't contain apt configuration"
            }
            try {
                project.configurations.getByName("provided").resolve().each { dependency ->
                    mAllSubProjectsAptDependencies.get(project.name).add(dependency)
                }
            } catch (UnknownConfigurationException e) {
                println "${project.name} doesn't contain provided configuration"
            }
            try {
                project.configurations.getByName("${mBuildVariant}Compile").resolve().each { dependency ->
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
            mAllSubProjectsInternalDependencies.put(project.name, new HashSet<String>())
            project.configurations.compile.resolve().each { dependency ->
                // for each of its compile dependency, if dependency's path start with
                // **another** sub project's build path, it's an internal dependency(project),
                // otherwise, it's an external dependency, whether maven/m2/local jar/aar.
                boolean isProjectDep = false
                String projectDep = ""
                for (Project subProject : mRootProject.subprojects) {
                    if (!project.projectDir.equals(
                            subProject.projectDir) && dependency.absolutePath.
                            startsWith(subProject.buildDir.absolutePath)) {
                        isProjectDep = true
                        projectDep = subProject.name
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

            try {
                project.configurations.getByName("${mBuildVariant}Compile").resolve().each { dependency ->
                    boolean isProjectDep = false
                    String projectDep = ""
                    for (Project subProject : mRootProject.subprojects) {
                        if (!project.projectDir.equals(
                                subProject.projectDir) && dependency.absolutePath.
                                startsWith(subProject.buildDir.absolutePath)) {
                            isProjectDep = true
                            projectDep = subProject.name
                            break
                        }
                    }
                    if (isProjectDep) {
                        println "${project.name}'s ${mBuildVariant}Compile dependency ${dependency.absolutePath} is an internal dependency, sub project: ${projectDep}"
                        mAllSubProjectsInternalDependencies.get(project.name).add(projectDep)
                    } else {
                        println "${project.name}'s ${mBuildVariant}Compile dependency ${dependency.absolutePath} is an external dependency"
                        mAllSubProjectsExternalDependencies.get(project.name).add(dependency)
                    }
                }
            } catch (Exception e) {
                println "${project.name} doesn't have ${mBuildVariant}Compile dependencies"
            }
        }
    }
}