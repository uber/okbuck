package com.uber.okbuck.core.model.base

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.BaseVariantOutput
import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.dependency.DependencyCache
import com.uber.okbuck.core.dependency.ExternalDependency
import com.uber.okbuck.core.dependency.VersionlessDependency
import com.uber.okbuck.core.model.android.AndroidLibTarget
import com.uber.okbuck.core.model.groovy.GroovyLibTarget
import com.uber.okbuck.core.model.java.JavaLibTarget
import com.uber.okbuck.core.model.jvm.JvmTarget
import com.uber.okbuck.core.util.FileUtil
import com.uber.okbuck.core.util.ProjectUtil
import groovy.transform.EqualsAndHashCode
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.UnknownConfigurationException

@EqualsAndHashCode
class Scope {

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

    Set<String> getAnnotationProcessors() {
        return ((firstLevel.collect {
            depCache.getAnnotationProcessors(it)
        } + targetDeps.collect { Target target ->
            (List<String>) target.getProp(project.rootProject.okbuck.annotationProcessors, null)
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

                if (!depCache.getTargetIdentifier(dependency)) {
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

            String targetIdentifier = depCache.getTargetIdentifier(dependency)
            if (targetIdentifier) {
                Project targetProject = project.project(targetIdentifier.replaceFirst("project ", ""))
                Target target = getTargetForOutput(targetProject, artifact.file)
                if (target) {
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
            external.add(ExternalDependency.fromLocal(localDep))
        }
    }

    @SuppressWarnings("GrReassignedInClosureLocalVar")
    static Target getTargetForOutput(Project targetProject, File output) {
        Target result = null
        ProjectType type = ProjectUtil.getType(targetProject)
        switch (type) {
            case ProjectType.ANDROID_LIB:
                def baseVariants = targetProject.android.libraryVariants
                baseVariants.all { BaseVariant baseVariant ->
                    def variant = baseVariant.outputs.find { BaseVariantOutput out ->
                        (out.outputFile == output)
                    }
                    if (variant != null) {
                        result = new AndroidLibTarget(targetProject, variant.name)
                    }
                }
                break
            case ProjectType.GROOVY_LIB:
                result = new GroovyLibTarget(targetProject, JvmTarget.MAIN)
                break
            case ProjectType.JAVA_APP:
            case ProjectType.JAVA_LIB:
                result = new JavaLibTarget(targetProject, JvmTarget.MAIN)
                break
            default:
                result = null
        }
        return result
    }
}
