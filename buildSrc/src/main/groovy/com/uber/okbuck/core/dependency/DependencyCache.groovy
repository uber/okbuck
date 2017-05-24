package com.uber.okbuck.core.dependency

import com.uber.okbuck.core.util.FileUtil
import groovy.transform.Synchronized
import org.apache.commons.io.IOUtils
import org.gradle.api.Project
import org.gradle.api.artifacts.ComponentSelection
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.plugins.ide.internal.IdeDependenciesExtractor

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.jar.JarEntry
import java.util.jar.JarFile

class DependencyCache {

    // These are used by conventions such as gradleApi() and localGroovy() and are whitelisted
    private static final Set<String> WHITELIST_LOCAL_PATTERNS = ['generated-gradle-jars/gradle-api-', 'wrapper/dists']

    final Project rootProject
    final File cacheDir

    final boolean useFullDepName
    final boolean fetchSources
    final boolean extractLintJars

    private final Configuration superConfiguration
    private final Map<VersionlessDependency, String> lintJars = [:]
    private final Map<VersionlessDependency, Set<String>> annotationProcessors = [:]

    private final Map<VersionlessDependency, ExternalDependency> externalDeps = [:]
    private final Map<VersionlessDependency, ProjectDependency> projectDeps = [:]

    DependencyCache(
            String name,
            Project rootProject,
            String cacheDirPath,
            Set<Configuration> configurations,
            String buckFile = null,
            boolean cleanup = true,
            boolean useFullDepName = false,
            boolean fetchSources = false,
            boolean extractLintJars = false,
            Set<Project> depProjects = null) {

        this.rootProject = rootProject
        this.cacheDir = new File(rootProject.projectDir, cacheDirPath)
        this.cacheDir.mkdirs()

        superConfiguration = createSuperConfiguration(rootProject, "${name}DepCache" as String, configurations)

        if (buckFile) {
            FileUtil.copyResourceToProject(buckFile, new File(cacheDir, "BUCK"))
        }

        this.useFullDepName = useFullDepName
        this.fetchSources = fetchSources
        this.extractLintJars = extractLintJars
        build(cleanup, depProjects)
    }

    String get(VersionlessDependency dependency) {
        ExternalDependency externalDependency = externalDeps.get(dependency)
        if (externalDependency == null) {
            throw new IllegalStateException("Could not find dependency path for ${dependency}")
        }

        File cachedCopy = new File(cacheDir, externalDependency.getCacheName(useFullDepName))
        return FileUtil.getRelativePath(rootProject.projectDir, cachedCopy)
    }

    @Synchronized
    Set<String> getAnnotationProcessors(VersionlessDependency dependency) {
        ExternalDependency greatest = externalDeps.get(dependency)
        if (greatest.depFile.name.endsWith(".jar")) {
            Set<String> processors = annotationProcessors.get(greatest)
            if (!processors) {
                File cachedCopy = new File(cacheDir, greatest.getCacheName(useFullDepName))
                processors = getAnnotationProcessorsFile(cachedCopy).text.split('\n')
                annotationProcessors.put(greatest, processors)
            }
            return processors
        } else {
            return []
        }
    }

    private void build(boolean cleanup, Set<Project> depProjects) {
        Set<File> resolvedFiles = [] as Set

        if (depProjects) {
            depProjects.each { Project project ->
                ProjectDependency dependency = new ProjectDependency(project)
                projectDeps.put(dependency, dependency)
            }
        }

        superConfiguration.resolvedConfiguration.resolvedArtifacts.each { ResolvedArtifact artifact ->
            ExternalDependency dependency = new ExternalDependency(artifact.moduleVersion.id, artifact.file,
                    artifact.classifier)
            if (!projectDeps.containsKey(dependency.withoutClassifier())) {
                externalDeps.put(dependency, dependency)
            }
            resolvedFiles.add(artifact.file)
        }

        superConfiguration.files.findAll { File resolved ->
            !resolvedFiles.contains(resolved)
        }.each { File localDep ->
            ExternalDependency localDependency = ExternalDependency.fromLocal(localDep)
            externalDeps.put(localDependency, localDependency)
        }

        resolvedFiles = null

        // Download sources if enabled
        if (fetchSources) {
            new IdeDependenciesExtractor().extractRepoFileDependencies(
                    rootProject.dependencies,
                    [superConfiguration],
                    [],
                    true,
                    false)
        }

        Set<File> cachedCopies = [] as Set

        externalDeps.each { _, ExternalDependency e ->
            File cachedCopy = new File(cacheDir, e.getCacheName(useFullDepName))

            // Copy the file into the cache
            if (!cachedCopy.exists()) {
                Files.createSymbolicLink(cachedCopy.toPath(), e.depFile.toPath())
            }
            cachedCopies.add(cachedCopy)

            // Extract Lint Jars
            if (extractLintJars && cachedCopy.name.endsWith(".aar")) {
                File lintJar = getPackagedLintJarFrom(cachedCopy)
                if (lintJar != null) {
                    String lintJarPath = FileUtil.getRelativePath(rootProject.projectDir, lintJar)
                    lintJars.put(e, lintJarPath)
                    cachedCopies.add(lintJar)
                }
            }

            // Fetch Sources
            if (fetchSources) {
                File sources = fetchSourcesFor(e)
                if (sources != null) {
                    cachedCopies.add(sources)
                }
            }
        }

        // cleanup
        if (cleanup) {
            (cacheDir.listFiles(new FileFilter() {

                @Override
                boolean accept(File pathname) {
                    return pathname.isFile() && (pathname.name.endsWith(".jar") || pathname.name.endsWith(".aar"))
                }
            }) - cachedCopies).each { File f ->
                Files.deleteIfExists(f.toPath())
            }
        }
    }

    Project getProject(VersionlessDependency dependency) {
        ProjectDependency targetDependency = projectDeps.get(dependency.withoutClassifier())
        if (targetDependency) {
            return targetDependency.project
        } else {
            return null
        }
    }

    boolean isWhiteListed(File depFile) {
        return WHITELIST_LOCAL_PATTERNS.find { depFile.absolutePath.contains(it) } != null
    }

    private static Configuration createSuperConfiguration(Project project,
                                                          String superConfigName,
                                                          Set<Configuration> configurations) {
        Configuration superConfiguration = project.configurations.maybeCreate(superConfigName)
        configurations.each {
            superConfiguration.dependencies.addAll(it.incoming.dependencies.findAll {
                !(it instanceof org.gradle.api.artifacts.ProjectDependency)
            })
        }

        superConfiguration.dependencies.each { Dependency dependency ->
            String version = dependency.version
            if (version && (version.contains("+"))) {
                err(project, "${dependency.group}:${dependency.name}:${version} : " +
                        "Please do not use dynamic version dependencies. They can cause hard to reproduce builds")
            }
        }

        superConfiguration.resolutionStrategy.componentSelection.all { ComponentSelection selection ->
            String version = selection.candidate.version
            if (version.contains("-SNAPSHOT")) {
                err(project, "${selection.candidate.displayName} : " +
                        "Please do not use snapshot version dependencies. They can cause hard to reproduce builds")
            }
        }

        return superConfiguration
    }

    private static err(Project project, String message) {
        if (project.okbuck.failOnChangingDependencies) {
            throw new IllegalStateException(message)
        } else {
            println "\n${message}\n"
        }
    }

    String getLintJar(VersionlessDependency dependency) {
        return lintJars.get(dependency)
    }

    private File fetchSourcesFor(ExternalDependency dependency) {
        // We do not have sources for these dependencies
        if (isWhiteListed(dependency.depFile)) {
            return null
        }

        File cachedCopy = new File(cacheDir, dependency.getSourceCacheName(useFullDepName))

        if (!cachedCopy.exists()) {
            String sourcesJarName = dependency.getSourceCacheName(false)
            File sourcesJar = new File(dependency.depFile.parentFile, sourcesJarName)

             if (!sourcesJar.exists()) {
                def sourceJars = rootProject.fileTree(
                        dir: dependency.depFile.parentFile.parentFile.absolutePath,
                        includes: ["**/${sourcesJarName}"]) as List
                if (sourceJars.size() == 1) {
                    sourcesJar = sourceJars[0]
                } else if (sourceJars.size() > 1) {
                    throw new IllegalStateException("Found multiple source jars: ${sourceJars} for ${dependency}")
                }
            }
            if (sourcesJar.exists()) {
                Files.createSymbolicLink(cachedCopy.toPath(), sourcesJar.toPath())
            }
        }

        return cachedCopy.exists() ? cachedCopy : null
    }

    static File getPackagedLintJarFrom(File aar) {
        File lintJar = new File(aar.parentFile, aar.name.replaceFirst(/\.aar$/, '-lint.jar'))
        if (lintJar.exists()) {
            return lintJar
        }
        FileSystem zipFile = FileSystems.newFileSystem(aar.toPath(), null)
        Path packagedLintJar = zipFile.getPath("lint.jar")
        if (Files.exists(packagedLintJar)) {
            Files.copy(packagedLintJar, lintJar.toPath(), StandardCopyOption.REPLACE_EXISTING)
            return lintJar
        } else {
            return null
        }
    }

    static File getAnnotationProcessorsFile(File jar) {
        File processors = new File(jar.parentFile, jar.name.replaceFirst(/\.jar$/, '.processors'))
        if (processors.exists()) {
            return processors
        }

        JarFile jarFile = new JarFile(jar)
        JarEntry jarEntry = (JarEntry) jarFile.getEntry("META-INF/services/javax.annotation.processing.Processor")
        if (jarEntry) {
            List<String> processorClasses = IOUtils.toString(jarFile.getInputStream(jarEntry))
                    .trim().split("\\n").findAll { String entry ->
                !entry.startsWith('#') && !entry.trim().empty // filter out comments and empty lines
            }
            processors.text = processorClasses.join('\n')
        } else {
            processors.createNewFile()
        }
        return processors
    }
}
