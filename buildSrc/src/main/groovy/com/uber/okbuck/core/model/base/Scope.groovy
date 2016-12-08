package com.uber.okbuck.core.model.base

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.BaseVariantOutput
import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.dependency.DependencyCache
import com.uber.okbuck.core.dependency.ExternalDependency
import com.uber.okbuck.core.dependency.InValidDependencyException
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
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.UnknownConfigurationException
import org.gradle.api.internal.artifacts.Module
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
                            if (!depCache.isValid(dep)) {
                                throw new InValidDependencyException("${target.project} is not a valid project dependency")
                            }
                            targetDeps.add(target)
                        }
                    } else {
                        ExternalDependency dependency = new ExternalDependency(artifact.moduleVersion.id, dep)
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

                    ModuleVersionIdentifier identifier = getDepIdentifier(
                            localDepGroup,
                            FilenameUtils.getBaseName(localDep.name),
                            "1.0.0")

                    ExternalDependency dependency = new ExternalDependency(identifier, localDep)
                    external.add(dependency)
                    depCache.put(dependency)
                }
            } catch (InValidDependencyException e) {
                throw new IllegalStateException("Invalid dependency found for ${project} , ${validConfigurations}", e)
            } catch (UnknownConfigurationException ignored) {
            }
        }

        // Download sources if enabled
        if (depCache.fetchSources) {
            new IdeDependenciesExtractor().extractRepoFileDependencies(project.dependencies, validConfigurations, [], true, false)
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

    static ModuleVersionIdentifier getDepIdentifier(String group, String name, String version) {
        return new ModuleVersionIdentifier() {

            @Override
            String getVersion() {
                return version
            }

            @Override
            String getGroup() {
                return group
            }

            @Override
            String getName() {
                return name
            }

            @Override
            ModuleIdentifier getModule() {
                return null
            }
        }
    }
}
