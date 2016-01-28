/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Piasy
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
import com.github.piasy.okbuck.helper.FileUtil
import com.github.piasy.okbuck.helper.ProjectHelper
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * extract dependencies from Project, and output them with:
 * Map<Project, Map<String, Set<Dependency>>> : Project => FlavorVariant => Dependency,
 * Map<Project, Set<String>> : Project => annotation processors,
 * Map<Project, Set<File>> : Project => apt dependency,
 * */
public final class DependencyExtractor {
    private static Logger logger = Logging.getLogger(DependencyExtractor)

    private final Map<Project, Map<String, Set<Dependency>>> mFullDependencies
    private final Map<Project, Set<Dependency>> mAptDependencies
    private final Map<Project, Set<String>> mAnnotationProcessors

    private final Project mRootProject

    public DependencyExtractor(Project rootProject) {
        mRootProject = rootProject
        mFullDependencies = new HashMap<>()
        mAptDependencies = new HashMap<>()
        mAnnotationProcessors = new HashMap<>()
    }

    /**
     * extract dependencies, but don't do any analyze jobs
     * */
    void extract() {
        // extract apt and provided dependencies
        extractAptDependencies()

        // extract annotation processors
        extractAnnotationProcessors()

        // extract compile dependencies
        extractCompileDependencies()

        printExtractResult()
    }

    Map<Project, Map<String, Set<Dependency>>> getFullDependencies() {
        return mFullDependencies
    }

    Map<Project, Set<Dependency>> getAptDependencies() {
        return mAptDependencies
    }

    Map<Project, Set<String>> getAnnotationProcessors() {
        return mAnnotationProcessors
    }

    private extractAptDependencies() {
        for (Project project : mRootProject.subprojects) {
            if (ProjectHelper.getSubProjectType(project) == ProjectHelper.ProjectType.Unknown) {
                continue
            }
            mAptDependencies.put(project, new HashSet<Dependency>())
            try {
                for (File dependency : project.configurations.getByName("apt").resolve()) {
                    mAptDependencies.get(project).
                            add(Dependency.fromLocalFile(mRootProject.projectDir, dependency))
                }
            } catch (Exception e) {
                logger.info "${project.name} doesn't contain apt configuration"
            }
            try {
                for (File dependency : project.configurations.getByName("provided").resolve()) {
                    mAptDependencies.get(project).
                            add(Dependency.fromLocalFile(mRootProject.projectDir, dependency))
                }
            } catch (Exception e) {
                logger.info "${project.name} doesn't contain provided configuration"
            }
        }
    }

    private extractAnnotationProcessors() {
        for (Project project : mRootProject.subprojects) {
            if (ProjectHelper.getSubProjectType(project) == ProjectHelper.ProjectType.Unknown) {
                continue
            }
            mAnnotationProcessors.put(project, new HashSet<String>())
            for (Dependency dependency : mAptDependencies.get(project)) {
                try {
                    JarFile jar = new JarFile(dependency.depFile)
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
                    logger.debug "${dependency.depFile.name} doesn't have annotation processor"
                }
            }
        }
    }

    private extractCompileDependencies() {
        for (Project project : mRootProject.subprojects) {
            ProjectHelper.ProjectType type = ProjectHelper.getSubProjectType(project)
            if (type == ProjectHelper.ProjectType.Unknown) {
                continue
            }
            // for each sub project
            mFullDependencies.put(project, new HashMap<String, Set<Dependency>>())
            extractOneConfiguration(project, "main", "compile")

            if (type == ProjectHelper.ProjectType.JavaLibProject) {
                continue
            }
            extractOneConfiguration(project, "debug", "debugCompile")
            extractOneConfiguration(project, "release", "releaseCompile")

            if (ProjectHelper.exportFlavor(project)) {
                Map<String, ProductFlavor> flavorMap = ProjectHelper.getProductFlavors(project)
                for (String flavor : flavorMap.keySet()) {
                    extractOneConfiguration(project, flavor, "${flavor}Compile")
                }
            }
        }
    }

    private void extractOneConfiguration(Project project, String flavor, String configuration) {
        mFullDependencies.get(project).put(flavor, new HashSet<Dependency>())
        mergeDependencies(mRootProject, mFullDependencies.get(project).get(flavor),
                extractFinalDependencyFiles(project, configuration),
                extractAndFlatResolvedDependencies(project, configuration))
    }

    private static Set<File> extractFinalDependencyFiles(Project project, String configuration) {
        Set<File> deps = new HashSet<>()
        try {
            for (File dependency : project.configurations.getByName(configuration).resolve()) {
                deps.add(dependency)
            }
        } catch (Exception e) {
            logger.info "${project.name} doesn't have ${configuration} dependencies"
        }
        return deps
    }

    private static Map<ResolvedDependency, File> extractAndFlatResolvedDependencies(
            Project project, String configuration
    ) {
        Map<ResolvedDependency, File> deps = new HashMap<>()
        try {
            for (ResolvedDependency dependency : project.configurations.getByName(
                    configuration).resolvedConfiguration.firstLevelModuleDependencies) {
                flatResolvedDependencies(deps, dependency)
            }
        } catch (Exception e) {
            logger.info "${project.name} doesn't have ${configuration} dependencies"
        }
        return deps
    }

    private static void flatResolvedDependencies(
            Map<ResolvedDependency, File> deps, ResolvedDependency dependency
    ) {
        deps.put(dependency, dependency.moduleArtifacts[0].file)
        for (ResolvedDependency child : dependency.children) {
            flatResolvedDependencies(deps, child)
        }
    }

    /**
     * resolvedDeps.valueSet is the `true subset` of depFiles
     * */
    private void mergeDependencies(
            Project rootProject, Set<Dependency> dependencies, Set<File> depFiles,
            Map<ResolvedDependency, File> resolvedDeps
    ) {
        for (ResolvedDependency dep : resolvedDeps.keySet()) {
            File depFile = resolvedDeps.get(dep)
            Project moduleDep = ProjectHelper.getModuleDependencyProject(rootProject, depFile)
            if (moduleDep != null) {
                dependencies.add(Dependency.fromModule(
                        mRootProject.projectDir, depFile, moduleDep,
                        FileUtil.getFlavorOfModuleFromLocalDepFile(resolvedDeps.get(dep)),
                        FileUtil.getVariantOfModuleFromLocalDepFile(resolvedDeps.get(dep))))
            } else {
                dependencies.add(Dependency.fromMavenDependency(
                        mRootProject.projectDir, depFile, dep))
            }
            depFiles.remove(depFile)
        }
        for (File depFile : depFiles) {
            dependencies.add(Dependency.fromLocalFile(mRootProject.projectDir, depFile))
        }
    }

    private void printExtractResult() {
        logger.debug "############################ mFullDependencies start ############################"
        for (Project project : mFullDependencies.keySet()) {
            logger.debug "<<<<< ${project.name}"
            for (String flavor : mFullDependencies.get(project).keySet()) {
                logger.debug "\t<<< ${flavor}"
                for (Dependency dependency : mFullDependencies.get(project).get(flavor)) {
                    logger.debug "\t\t${dependency.toString()}"
                }
                logger.debug "\t>>>"
            }
            logger.debug ">>>>>"
        }
        logger.debug "############################ mFullDependencies end ##############################"

        logger.debug "############################ mAptDependencies start #############################"
        for (Project project : mAptDependencies.keySet()) {
            logger.debug "<<<<< ${project.name}"
            for (Dependency dependency : mAptDependencies.get(project)) {
                logger.debug "\t\t${dependency.depFile.absolutePath}"
            }
            logger.debug ">>>>>"
        }
        logger.debug "############################ mAptDependencies end ###############################"

        logger.debug "############################ mAnnotationProcessors start ########################"
        for (Project project : mAnnotationProcessors.keySet()) {
            logger.debug "<<<<< ${project.name}"
            for (String processor : mAnnotationProcessors.get(project)) {
                logger.debug "\t\t ${processor}"
            }
            logger.debug ">>>>>"
        }
        logger.debug "############################ mAnnotationProcessors end ##########################"
    }
}