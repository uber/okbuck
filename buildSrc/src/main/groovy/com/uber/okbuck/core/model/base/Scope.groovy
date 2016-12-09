package com.uber.okbuck.core.model.base

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.BaseVariantOutput
import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.dependency.DependencyCache
import com.uber.okbuck.core.dependency.ExternalDependency
import com.uber.okbuck.core.model.android.AndroidLibTarget
import com.uber.okbuck.core.model.groovy.GroovyLibTarget
import com.uber.okbuck.core.model.java.JavaLibTarget
import com.uber.okbuck.core.model.jvm.JvmTarget
import com.uber.okbuck.core.util.FileUtil
import com.uber.okbuck.core.util.ProjectUtil
import com.uber.okbuck.extension.OkBuckExtension
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

    Set<String> getExternalDepsWithFullPath() {
        external.collect { ExternalDependency dependency ->
            dependency.depFile.path
        }
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
                        Target target = getTargetForOutput(project.rootProject, dep)
                        if (target != null && target.project != project) {
                            targetDeps.add(target)
                        }
                    } else {
                        external.add(new ExternalDependency(artifact.moduleVersion.id, dep))
                    }
                }

                configuration.files.findAll { File resolved ->
                    !resolvedFiles.contains(resolved)
                }.each { File localDep ->
                    external.add(ExternalDependency.fromLocal(localDep))
                }
            } catch (UnknownConfigurationException ignored) {
            }
        }
    }

    @SuppressWarnings("GrReassignedInClosureLocalVar")
    static Target getTargetForOutput(Project rootProject, File output) {
        Target result = null
        OkBuckExtension okbuck = rootProject.okbuck
        Project project = okbuck.buckProjects.find { Project project ->
            FilenameUtils.directoryContains(project.buildDir.absolutePath, output.absolutePath)
        }

        if (project != null) {
            ProjectType type = ProjectUtil.getType(project)
            switch (type) {
                case ProjectType.ANDROID_LIB:
                    def baseVariants = project.android.libraryVariants
                    baseVariants.all { BaseVariant baseVariant ->
                        def variant = baseVariant.outputs.find { BaseVariantOutput out ->
                            (out.outputFile == output)
                        }
                        if (variant != null) {
                            result = new AndroidLibTarget(project, variant.name)
                        }
                    }
                    break
                case ProjectType.GROOVY_LIB:
                    result = new GroovyLibTarget(project, JvmTarget.MAIN)
                    break
                case ProjectType.JAVA_APP:
                case ProjectType.JAVA_LIB:
                    result = new JavaLibTarget(project, JvmTarget.MAIN)
                    break
                default:
                    result = null
            }
        }
        return result
    }
}
