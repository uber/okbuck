package com.uber.okbuck.core.model.base

import com.android.build.api.attributes.VariantAttr
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
import com.uber.okbuck.extension.OkBuckExtension
import groovy.transform.EqualsAndHashCode
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Attribute
import org.jetbrains.annotations.Nullable
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.ConcurrentHashMap

@EqualsAndHashCode
class Scope {

    private static final Logger LOG = LoggerFactory.getLogger(Scope)

    final String resourcesDir
    final Set<String> sources
    final Set<Target> targetDeps = new HashSet<>()

    List<String> jvmArgs
    DependencyCache depCache

    protected final Project project
    final Set<ExternalDependency> external = new HashSet<>()

    protected Scope(Project project,
                    Set<Configuration> configurations,
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
        Set<Configuration> useful = DependencyUtils.useful(project, configurations)
        String key = useful.collect { it.name }.toSorted().join("_")
        return ProjectUtil.getScopes(project)
                .computeIfAbsent(project, { new ConcurrentHashMap<>() })
                .computeIfAbsent(key,
                { new Scope(project, useful, sourceDirs, resDir, jvmArguments, depCache) })
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
            OkBuckExtension okBuckExtension = project.rootProject.okbuck
            target.getProp(okBuckExtension.annotationProcessors, ImmutableSet.of())
        }).flatten() as Set<String>).findAll { !it.empty }
    }

    private static Set<ResolvedArtifactResult> getArtifacts(
            Configuration configuration, String value) {
        return configuration.getIncoming().artifactView({ config ->
            config.attributes({ container ->
                container.getAttribute(VariantAttr.ATTRIBUTE)
                container.attribute(Attribute.of("artifactType", String.class), value);
            })
        }).getArtifacts().getArtifacts()
    }

    private static Set<ResolvedArtifactResult> getArtifacts(Set<Configuration> configurations) {
        Set<ResolvedArtifactResult> artifactResults = configurations.collect { config ->
            return ImmutableSet.builder()
                    .addAll(getArtifacts(config, "aar"))
                    .addAll(getArtifacts(config, "jar"))
                    .build()

        }.flatten() as Set<ResolvedArtifactResult>

        artifactResults = artifactResults.findAll { it -> it.file.name != 'classes.jar' }

        return artifactResults
    }

    private void extractConfigurations(Set<Configuration> configurations) {
        if (configurations.size() == 0) {
            return
        }

        boolean resolveDups = configurations.size() > 1

        SortedSetMultimap<VersionlessDependency, ExternalDependency> greatest
        if (resolveDups) {
            greatest = MultimapBuilder.hashKeys().treeSetValues().build()
        }


        Set<ResolvedArtifactResult> artifacts = getArtifacts(configurations)

        Set<ComponentIdentifier> artifactIds = new HashSet<>()
        artifacts.each { ResolvedArtifactResult artifact ->
            if (!DependencyUtils.isConsumable(artifact.file)) {
                return
            }
            ComponentIdentifier identifier = artifact.id.componentIdentifier
            artifactIds.add(identifier)
            if (identifier instanceof ProjectComponentIdentifier) {

                String variant = artifact.variant.attributes.getAttribute(VariantAttr.ATTRIBUTE)
                targetDeps.add(ProjectUtil.getTargetForOutput(
                        project.project(identifier.projectPath), variant))

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

        // Download sources if needed
        if (project.rootProject.okbuck.intellij.sources) {
            ProjectUtil.downloadSources(project, artifactIds)
        }

        if (resolveDups) {
            greatest.keySet().each {
                external.add(greatest.get(it).first())
            }
        }

        if (project.rootProject.okbuck.intellij.sources) {
            Set<ComponentIdentifier> ids = artifacts.collect { artifact ->
                artifact.getId().getComponentIdentifier()
            }
            ProjectUtil.downloadSources(project, ids)
        }
    }

}
