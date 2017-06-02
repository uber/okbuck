package com.uber.okbuck.core.model.base

import com.uber.okbuck.core.dependency.DependencyCache
import com.uber.okbuck.core.dependency.ExternalDependency
import com.uber.okbuck.core.dependency.VersionlessDependency
import com.uber.okbuck.core.util.FileUtil
import com.uber.okbuck.core.util.ProjectUtil
import groovy.transform.EqualsAndHashCode
import org.apache.commons.io.FilenameUtils
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.UnknownConfigurationException
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult

@EqualsAndHashCode
class Scope {
    final String resourcesDir
    final Set<String> sources
    final Set<Target> targetDeps = [] as Set

    List<String> jvmArgs
    DependencyCache depCache

    protected final Project project
    protected final Set<VersionlessDependency> external = [] as Set

    Scope(Project project,
          Collection<String> configurations,
          Set<File> sourceDirs = [],
          File resDir = null,
          List<String> jvmArguments = [],
          DependencyCache depCache = ProjectUtil.getDependencyCache(project)) {

        this.project = project
        sources = FileUtil.getIfAvailable(project, sourceDirs)
        resourcesDir = FileUtil.getIfAvailable(project, resDir)
        jvmArgs = jvmArguments
        this.depCache = depCache

        extractConfigurations(configurations)
    }

    Set<String> getExternalDeps() {
        return external.collect { VersionlessDependency dependency ->
            depCache.get(dependency)
        }
    }

    Set<String> getPackagedLintJars() {
        return external.collect { VersionlessDependency dependency ->
            depCache.getLintJar(dependency)
        }.findAll { String lintJar ->
            lintJar != null
        }
    }

    Set<String> getAnnotationProcessors() {
        return ((external.collect {
            depCache.getAnnotationProcessors(it)
        } + targetDeps.collect { Target target ->
            target.getProp(project.rootProject.okbuck.annotationProcessors as Map, []) as Set<String>
        }).flatten() as Set<String>).findAll { !it.empty }
    }

    private void extractConfigurations(Collection<String> configurations) {
        Set<Configuration> validConfigurations = []
        configurations.each { String configName ->
            try {
                Configuration configuration = project.configurations.getByName(configName)
                validConfigurations.add(configuration)
            } catch (UnknownConfigurationException ignored) { }
        }
        validConfigurations = useful(validConfigurations)

        Set<ResolvedArtifactResult> artifacts = validConfigurations.collect {
            it.incoming.artifacts.artifacts
        }.flatten() as Set<ResolvedArtifactResult>

        artifacts.each { ResolvedArtifactResult artifact ->
            ComponentIdentifier identifier = artifact.id.componentIdentifier
            if (identifier instanceof ProjectComponentIdentifier) {
                targetDeps.add(ProjectUtil.getTargetForOutput(project.project(identifier.projectPath), artifact.file))
            } else if (identifier instanceof ModuleComponentIdentifier) {
                external.add(new ExternalDependency(
                        identifier.group,
                        identifier.module,
                        identifier.version,
                        artifact.file
                ))
            } else {
                if (!FilenameUtils.directoryContains(project.rootProject.projectDir.absolutePath,
                        artifact.file.absolutePath) && !depCache.isWhiteListed(artifact.file)) {
                    throw new IllegalStateException("Local dependencies should be under project root. Dependencies " +
                            "outside the project can cause hard to reproduce builds. Please move dependency: ${localDep} " +
                            "inside ${project.rootProject.projectDir}")
                }
                external.add(ExternalDependency.fromLocal(artifact.file))
            }
        }
    }

    static Set<Configuration> useful(Set<Configuration> configurations) {
        return configurations.findAll { Configuration configuration ->
            !configuration.dependencies.empty || !configurations.containsAll(configuration.extendsFrom)
        }
    }
}
