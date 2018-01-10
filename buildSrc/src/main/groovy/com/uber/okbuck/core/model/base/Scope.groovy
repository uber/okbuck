package com.uber.okbuck.core.model.base

import com.android.build.api.attributes.VariantAttr
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.uber.okbuck.core.dependency.DependencyCache
import com.uber.okbuck.core.dependency.DependencyUtils
import com.uber.okbuck.core.dependency.ExternalDependency
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
import org.gradle.api.specs.Spec
import org.jetbrains.annotations.Nullable

import java.util.concurrent.ConcurrentHashMap

@EqualsAndHashCode
class Scope {

    final String resourcesDir
    final Set<String> sources
    final Set<Target> targetDeps = new HashSet<>()

    List<String> jvmArgs
    DependencyCache depCache

    protected final Project project
    final Set<ExternalDependency> external = new HashSet<>()

    protected Scope(Project project,
                    Configuration configuration,
                    Set<File> sourceDirs,
                    @Nullable File resDir,
                    List<String> jvmArguments,
                    DependencyCache depCache = ProjectUtil.getDependencyCache(project)) {

        this.project = project
        sources = FileUtil.available(project, sourceDirs)
        resourcesDir = resDir ? FileUtil.available(project, ImmutableSet.of(resDir))[0] : null
        jvmArgs = jvmArguments
        this.depCache = depCache

        if (configuration) {
            extractConfiguration(configuration)
        }
    }

    static Scope from(Project project,
                      String configuration,
                      Set<File> sourceDirs = ImmutableSet.of(),
                      @Nullable File resDir = null,
                      List<String> jvmArguments = ImmutableList.of(),
                      DependencyCache depCache = ProjectUtil.getDependencyCache(project)) {
        Configuration useful = DependencyUtils.useful(project, configuration)
        return from(project, useful, sourceDirs, resDir, jvmArguments, depCache)
    }

    static Scope from(Project project,
                      Configuration configuration,
                      Set<File> sourceDirs = ImmutableSet.of(),
                      @Nullable File resDir = null,
                      List<String> jvmArguments = ImmutableList.of(),
                      DependencyCache depCache = ProjectUtil.getDependencyCache(project)) {
        Configuration useful = DependencyUtils.useful(configuration)
        String key = useful ? useful.name : "--none--"
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
            Configuration configuration, String value, Spec<ComponentIdentifier> filter) {
        return configuration.getIncoming().artifactView({ config ->
            config.attributes({ container ->
                container.attribute(Attribute.of("artifactType", String.class), value);
            })
            config.componentFilter(filter)
        }).getArtifacts().getArtifacts()
    }

    /*
     * Resolves the configuration using the Variant Attribute and returns the aar/jar artifacts
     */
    private static Set<ResolvedArtifactResult> getArtifacts(
            Configuration configuration,
            Spec<ComponentIdentifier> filter,
            ImmutableList<String> artifactTypes) {

        ImmutableSet.Builder<ResolvedArtifactResult> artifactResultsBuilder =
                ImmutableSet.builder();

        artifactTypes.each { artifactType ->
            artifactResultsBuilder.addAll(getArtifacts(configuration, artifactType, filter))
        }

        Set<ResolvedArtifactResult> artifactResults =
                artifactResultsBuilder.build().findAll { it -> it.file.name != 'classes.jar' }

        return artifactResults
    }

    private void extractConfiguration(Configuration configuration) {
        Set<ComponentIdentifier> artifactIds = new HashSet<>()

        Spec<ComponentIdentifier> filter = new Spec<ComponentIdentifier>() {
            @Override
            boolean isSatisfiedBy(ComponentIdentifier componentIdentifier) {
                return componentIdentifier instanceof ProjectComponentIdentifier
            }
        }

        Set<ResolvedArtifactResult> artifacts =
                getArtifacts(
                        configuration,
                        filter,
                        ImmutableList.of("jar"))

        artifacts.each { ResolvedArtifactResult artifact ->
            if (!DependencyUtils.isConsumable(artifact.file)) {
                return
            }

            ProjectComponentIdentifier identifier = artifact.id.componentIdentifier
            String variant = artifact.variant.attributes.getAttribute(VariantAttr.ATTRIBUTE)
            targetDeps.add(ProjectUtil.getTargetForVariant(
                    project.project(identifier.projectPath), variant))
        }

        filter = new Spec<ComponentIdentifier>() {
            @Override
            boolean isSatisfiedBy(ComponentIdentifier componentIdentifier) {
                return !(componentIdentifier instanceof ProjectComponentIdentifier)
            }
        }

        artifacts = getArtifacts(
                configuration,
                filter,
                ImmutableList.of("aar", "jar"))

        artifacts.each { ResolvedArtifactResult artifact ->
            if (!DependencyUtils.isConsumable(artifact.file)) {
                return
            }

            ComponentIdentifier identifier = artifact.id.componentIdentifier
            artifactIds.add(identifier)

            if (identifier instanceof ModuleComponentIdentifier && identifier.version) {
                ExternalDependency externalDependency = new ExternalDependency(
                        identifier.group,
                        identifier.module,
                        identifier.version,
                        artifact.file
                )
                external.add(externalDependency)
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

        if (project.rootProject.okbuck.intellij.sources) {
            ProjectUtil.downloadSources(project, artifactIds)
        }
    }

}
