package com.uber.okbuck.core.model.base

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.collect.MultimapBuilder
import com.google.common.collect.SortedSetMultimap
import com.uber.okbuck.core.dependency.DependencyCache
import com.uber.okbuck.core.dependency.DependencyUtils
import com.uber.okbuck.core.dependency.ExternalDependency
import com.uber.okbuck.core.dependency.ExternalDependency.VersionlessDependency
import com.uber.okbuck.core.util.FileUtil
import com.uber.okbuck.core.util.ProjectUtil
import groovy.transform.EqualsAndHashCode
import jdk.nashorn.internal.ir.annotations.Immutable
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.UnknownConfigurationException
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.jetbrains.annotations.Nullable

@EqualsAndHashCode
class Scope {

    final String resourcesDir
    final Set<String> sources
    final Set<Target> targetDeps = [] as Set

    List<String> jvmArgs
    DependencyCache depCache

    protected final Project project
    final Set<ExternalDependency> external = [] as Set

    protected Scope(Project project,
                    Set<String> configurations,
                    Set<File> sourceDirs,
                    @Nullable File resDir,
                    List<String> jvmArguments,
                    DependencyCache depCache = ProjectUtil.getDependencyCache(project)) {

        this.project = project
        sources = FileUtil.available(project, sourceDirs)
        resourcesDir = resDir ? FileUtil.available(project, ImmutableSet.of(resDir))[0] : null
        jvmArgs = jvmArguments
        this.depCache = depCache

        extractConfigurations(configurations)
    }

    static Scope from(Project project,
                      Set<String> configurations,
                      Set<File> sourceDirs = ImmutableSet.of(),
                      @Nullable File resDir = null,
                      List<String> jvmArguments = ImmutableList.of(),
                      DependencyCache depCache = ProjectUtil.getDependencyCache(project)) {

        Map<String, Scope> projectScopes
        Scope projectScope

        Map<Project, Map<String, Scope>> scopes = ProjectUtil.getScopes(project)
        synchronized (scopes) {
            projectScopes = scopes.get(project)
            if (projectScopes == null) {
                projectScopes = new HashMap<>()
                scopes.put(project, projectScopes)
            }
        }

        String key = configurations.toSorted().join("_")
        synchronized (projectScopes) {
            projectScope = projectScopes.get(key)
            if (projectScope == null) {
                projectScope = new Scope(project, configurations, sourceDirs, resDir, jvmArguments, depCache)
                projectScopes.put(key, projectScope)
            }
        }

        return projectScope
    }

    Set<String> getExternalDeps() {
        return external.collect { ExternalDependency dependency ->
            depCache.get(dependency)
        }
    }

    Set<String> getPackagedLintJars() {
        return external.findAll { ExternalDependency dependency ->
            dependency.depFile.name.endsWith(".aar")
        }.collect { ExternalDependency dependency ->
            depCache.getLintJar(dependency)
        }.findAll { String lintJar ->
            !StringUtils.isEmpty(lintJar)
        }
    }

    Set<String> getAnnotationProcessors() {
        return ((external.collect {
            depCache.getAnnotationProcessors(it)
        } + targetDeps.collect { Target target ->
            target.getProp(project.rootProject.okbuck.annotationProcessors as Map, []) as Set<String>
        }).flatten() as Set<String>).findAll { !it.empty }
    }

    Set<File> getPackagedProguardConfigs() {
        external.collect {
            depCache.getProguardConfig(it)
        }.findAll {
            it != null
        }
    }

    private void extractConfigurations(Set<String> configurations) {
        Set<Configuration> validConfigurations = new HashSet<>()
        configurations.each { String configName ->
            try {
                validConfigurations.add(project.configurations.getByName(configName))
            } catch (UnknownConfigurationException ignored) {
            }
        }
        validConfigurations = DependencyUtils.useful(validConfigurations)
        boolean resolveDups = validConfigurations.size() > 1

        SortedSetMultimap<VersionlessDependency, ExternalDependency> greatest
        if (resolveDups) {
            greatest = MultimapBuilder.hashKeys().treeSetValues().build()
        }

        // Download sources if needed
        if (project.rootProject.okbuck.intellij.sources) {
            DependencyUtils.downloadSourceJars(project, validConfigurations)
        }

        Set<ResolvedArtifactResult> artifacts = validConfigurations.collect {
            it.incoming.artifacts.artifacts
        }.flatten() as Set<ResolvedArtifactResult>

        artifacts.each { ResolvedArtifactResult artifact ->
            if (!DependencyUtils.isConsumable(artifact.file)) {
                return
            }
            ComponentIdentifier identifier = artifact.id.componentIdentifier
            if (identifier instanceof ProjectComponentIdentifier) {
                targetDeps.add(ProjectUtil.getTargetForOutput(project.project(identifier.projectPath), artifact.file))
            } else if (identifier instanceof ModuleComponentIdentifier && identifier.version) {
                ExternalDependency externalDependency = new ExternalDependency(
                        identifier.group,
                        identifier.module,
                        identifier.version,
                        artifact.file
                )
                if (resolveDups) {
                    greatest.put(externalDependency.versionless, externalDependency)
                } else {
                    external.add(externalDependency)
                }
            } else {
                if (!FilenameUtils.directoryContains(project.rootProject.projectDir.absolutePath,
                        artifact.file.absolutePath) && !DependencyUtils.isWhiteListed(artifact.file)) {
                    throw new IllegalStateException("Local dependencies should be under project root. Dependencies " +
                            "outside the project can cause hard to reproduce builds. Please move dependency: " +
                            "${artifact.file} inside ${project.rootProject.projectDir}")
                }
                external.add(ExternalDependency.fromLocal(artifact.file))
            }
        }

        if (resolveDups) {
            greatest.keySet().each {
                external.add(greatest.get(it).first())
            }
        }
    }
}
