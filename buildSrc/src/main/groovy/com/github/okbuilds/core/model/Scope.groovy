package com.github.okbuilds.core.model

import com.github.okbuilds.core.dependency.ExternalDependency
import com.github.okbuilds.core.util.FileUtil
import com.github.okbuilds.core.util.ProjectUtil
import com.github.okbuilds.okbuck.OkBuckGradlePlugin
import groovy.transform.EqualsAndHashCode
import org.apache.commons.io.FilenameUtils
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.UnknownConfigurationException

@EqualsAndHashCode
class Scope {

    final String resourcesDir
    final Set<String> sources
    final Set<Target> targetDeps = [] as Set
    List<String> jvmArgs

    protected final Project project
    protected final Set<ExternalDependency> external = [] as Set

    Scope(Project project,
          Collection<String> configurations,
          Set<File> sourceDirs = [],
          File resDir = null,
          List<String> jvmArguments = []) {

        this.project = project
        sources = FileUtil.getAvailable(project, sourceDirs)
        resourcesDir = FileUtil.getAvailableFile(project, resDir)
        jvmArgs = jvmArguments

        extractConfigurations(configurations)
    }

    Set<String> getExternalDeps() {
        external.collect { ExternalDependency dependency ->
            OkBuckGradlePlugin.depCache.get(dependency)
        }
    }

    private void extractConfigurations(Collection<String> configurations) {
        configurations.each { String configName ->
            try {
                Configuration configuration = project.configurations.getByName(configName)
                Set<File> resolvedFiles = [] as Set
                configuration.resolvedConfiguration.resolvedArtifacts.each { ResolvedArtifact artifact ->
                    String identifier = artifact.id.componentIdentifier.displayName
                    File dep = artifact.file

                    resolvedFiles.add(dep)

                    if (identifier.contains(" ")) {
                        Target target = ProjectUtil.getTargetForOutput(project.gradle.rootProject, dep)
                        if (target != null) {
                            targetDeps.add(target)
                        }
                    } else {
                        ExternalDependency dependency = new ExternalDependency(identifier, dep, "${project.path.replaceFirst(':', '').replaceAll(':', '_')}:${FilenameUtils.getBaseName(dep.name)}:1.0.0")
                        external.add(dependency)
                        OkBuckGradlePlugin.depCache.put(dependency)
                    }
                }

                configuration.resolve().findAll { File resolved ->
                    !resolvedFiles.contains(resolved)
                }.each { File localDep ->
                    ExternalDependency dependency = new ExternalDependency(
                            "${project.path.replaceFirst(':', '').replaceAll(':', '_')}:${FilenameUtils.getBaseName(localDep.name)}:1.0.0",
                            localDep)
                    external.add(dependency)
                    OkBuckGradlePlugin.depCache.put(dependency)
                }
            } catch (UnknownConfigurationException ignored) {
            }
        }
    }
}
