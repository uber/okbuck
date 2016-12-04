package com.uber.okbuck.core.model.base

import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.dependency.DependencyCache
import com.uber.okbuck.core.dependency.ExternalDependency
import com.uber.okbuck.core.dependency.InValidDependencyException
import com.uber.okbuck.core.util.FileUtil
import com.uber.okbuck.core.util.ProjectUtil
import groovy.transform.EqualsAndHashCode
import org.apache.commons.io.FilenameUtils
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.UnknownConfigurationException
import org.gradle.plugins.ide.internal.IdeDependenciesExtractor

@EqualsAndHashCode
class Scope {

    private static final FILE_SEPARATOR = System.getProperty("file.separator")

    final String resourcesDir
    final Set<String> sources
    final Set<Target> targetDeps = [] as Set
    List<String> jvmArgs
    DependencyCache depCache

    protected final Project project
    protected final Set<ExternalDependency> external = [] as Set

    Scope(Project project,
          Collection<String> configurations,
          Set<File> sourceDirs = [],
          File resDir = null,
          List<String> jvmArguments = [],
          DependencyCache depCache = OkBuckGradlePlugin.depCache) {

        this.project = project
        sources = FileUtil.getAvailable(project, sourceDirs)
        resourcesDir = FileUtil.getAvailableFile(project, resDir)
        jvmArgs = jvmArguments
        this.depCache = depCache

        extractConfigurations(configurations)
    }

    Set<String> getExternalDeps() {
        external.collect { ExternalDependency dependency ->
            depCache.get(dependency)
        }
    }

    Set<String> getPackagedLintJars() {
        external.findAll { ExternalDependency dependency ->
            depCache.getLintJar(dependency) != null
        }.collect { ExternalDependency dependency ->
            depCache.getLintJar(dependency)
        }
    }

    private void extractConfigurations(Collection<String> configurations) {
        List<Configuration> validConfigurations = []
        configurations.each { String configName ->
            try {
                Configuration configuration = project.configurations.getByName(configName)
                validConfigurations.add(configuration)
                Set<File> resolvedFiles = [] as Set
                configuration.resolvedConfiguration.resolvedArtifacts.each { ResolvedArtifact artifact ->
                    String identifier = artifact.id.componentIdentifier.displayName
                    File dep = artifact.file

                    resolvedFiles.add(dep)

                    if (identifier.contains(" ")) {
                        Target target = ProjectUtil.getTargetForOutput(project.gradle.rootProject, dep)
                        if (target != null && target.project != project) {
                            if (!depCache.isValid(dep)) {
                                throw new InValidDependencyException("${target.project} is not a valid project dependency")
                            }
                            targetDeps.add(target)
                        }
                    } else {
                        ExternalDependency dependency = new ExternalDependency(identifier, dep, "${project.path.replaceFirst(':', '').replaceAll(':', '_')}:${FilenameUtils.getBaseName(dep.name)}:1.0.0")
                        external.add(dependency)
                        depCache.put(dependency)
                    }
                }

                configuration.files.findAll { File resolved ->
                    !resolvedFiles.contains(resolved)
                }.each { File localDep ->
                    String localDepGroup
                    if (FilenameUtils.directoryContains(project.rootDir.absolutePath, localDep.absolutePath)) {
                        localDepGroup = FileUtil.getRelativePath(project.rootDir, localDep).replaceAll(FILE_SEPARATOR, '_')
                    } else {
                        localDepGroup = project.path.replaceFirst(':', '').replaceAll(':', '_')
                    }
                    ExternalDependency dependency = new ExternalDependency(
                        "${localDepGroup}:${FilenameUtils.getBaseName(localDep.name)}:1.0.0",
                        localDep)
                    external.add(dependency)
                    depCache.put(dependency)
                }
            } catch (InValidDependencyException e) {
                throw new IllegalStateException("Invalid dependency found for ${project} , ${validConfigurations}", e)
            } catch(UnknownConfigurationException ignored) { }
        }

        // Download sources if enabled
        if (depCache.fetchSources) {
            new IdeDependenciesExtractor().extractRepoFileDependencies(project.dependencies, validConfigurations, [], true, false)
        }
    }
}
