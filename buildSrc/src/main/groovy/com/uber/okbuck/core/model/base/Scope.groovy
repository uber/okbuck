package com.uber.okbuck.core.model.base

import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.dependency.DependencyCache
import com.uber.okbuck.core.dependency.ExternalDependency
import com.uber.okbuck.core.dependency.VersionlessDependency
import com.uber.okbuck.core.util.FileUtil
import groovy.transform.EqualsAndHashCode
import org.apache.commons.io.FilenameUtils
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.UnknownConfigurationException

@EqualsAndHashCode
class Scope {

    // These are used by conventions such as gradleApi() and localGroovy() and are whitelisted
    private static final Set<String> WHITELIST_LOCAL_PATTERNS = ['generated-gradle-jars/gradle-api-', 'wrapper/dists']

    final String resourcesDir
    final Set<String> sources
    final Set<Target> targetDeps = [] as Set

    List<String> jvmArgs
    DependencyCache depCache

    protected final Project project
    protected final Set<VersionlessDependency> external = [] as Set
    protected final Set<VersionlessDependency> firstLevel = [] as Set

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

    Set<Target> getTargetAnnotationProcessorDeps() {
        return targetDeps.findAll { Target target ->
            target.getProp(project.rootProject.okbuck.annotationProcessors as Map, null) != null
        }
    }

    Set<String> getAnnotationProcessors() {
        return ((firstLevel.collect {
            depCache.getAnnotationProcessors(it)
        } + getTargetAnnotationProcessorDeps().collect { Target target ->
            target.getProp(project.rootProject.okbuck.annotationProcessors as Map, null) as Set<String>
        }).flatten() as Set<String>).findAll { it != null && !it.empty }
    }

    private void extractConfigurations(Collection<String> configurations) {
        List<Configuration> validConfigurations = []
        configurations.each { String configName ->
            try {
                Configuration configuration = project.configurations.getByName(configName)
                validConfigurations.add(configuration)
            } catch (UnknownConfigurationException ignored) {
            }
        }

        // get all first level external dependencies
        validConfigurations.collect {
            it.resolvedConfiguration.firstLevelModuleDependencies.each { ResolvedDependency resolvedDependency ->
                ResolvedArtifact artifact = resolvedDependency.moduleArtifacts[0]
                VersionlessDependency dependency = new VersionlessDependency(artifact.moduleVersion.id)

                if (!depCache.getProject(dependency)) {
                    firstLevel.add(dependency)
                }
            }
        }

        // get all resolved artifacts including transitives
        Set<ResolvedArtifact> artifacts = validConfigurations.collect {
            it.resolvedConfiguration.resolvedArtifacts
        }.flatten() as Set<ResolvedArtifact>

        Set<File> files = validConfigurations.collect {
            it.files
        }.flatten() as Set<File>

        Set<File> resolvedFiles = [] as Set
        artifacts.each { ResolvedArtifact artifact ->
            VersionlessDependency dependency = new VersionlessDependency(artifact.moduleVersion.id)

            Project targetProject = depCache.getProject(dependency)
            if (targetProject) {
                File artifactFile
                if (artifact.id.componentIdentifier.displayName.contains(" ")) {
                    artifactFile = artifact.file
                } else {
                    // Get the default configuration artifact file if an external
                    // artifact is getting resolved to a project dependency.
                    artifactFile = targetProject.configurations.getByName("default").allArtifacts.files.files[0]
                }

                Target target = TargetCache.getTargetForOutput(targetProject, artifactFile)
                if (target == null) {
                    throw new IllegalStateException("No such artifact: ${artifactFile} for ${targetProject} with " +
                            "artifact id: ${dependency}")
                } else if (target.project != project) {
                    targetDeps.add(target)
                }
            } else {
                external.add(dependency)
            }

            resolvedFiles.add(artifact.file)
        }

        // add remaining local jar/aar files to external dependency
        files.findAll { File resolved ->
            !resolvedFiles.contains(resolved)
        }.each { File localDep ->
            if (!FilenameUtils.directoryContains(project.rootProject.projectDir.absolutePath, localDep.absolutePath)
                    && WHITELIST_LOCAL_PATTERNS.find { localDep.absolutePath.contains(it) } == null) {
                throw new IllegalStateException("Local dependencies should be under project root. Dependencies " +
                        "outside the project can cause hard to reproduce builds. Please move dependency: ${localDep} " +
                        "inside ${project.rootProject.projectDir}")
            }
            external.add(ExternalDependency.fromLocal(localDep))
        }
    }
}
