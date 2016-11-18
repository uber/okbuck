package com.uber.okbuck.experimental.bazel

import com.uber.okbuck.core.dependency.DependencyCache
import com.uber.okbuck.core.dependency.ExternalDependency
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency

class DependencyCacheBuildFileWriter {
    private final DependencyCache cache

    DependencyCacheBuildFileWriter(DependencyCache cache) {
        this.cache = cache
    }

    void write(File buildFile) {
        buildFile.append """package(default_visibility = ["//visibility:public"])\n\n"""
        Set<String> visitedDependencies = new HashSet<>()
        cache.rootProject.okbuck.buckProjects.each { Project project ->
            project.configurations.getAsMap().each { String name, Configuration config ->
                if (true || name.toLowerCase().endsWith("compile")) {
                    config.resolvedConfiguration.getFirstLevelModuleDependencies().each {
                        ResolvedDependency resolvedDependency ->
                            writeBuildRules(resolvedDependency, buildFile, visitedDependencies)
                    }
                }
            }
        }
    }

    private writeBuildRules(
            ResolvedDependency resolvedDependency,
            File buildFile,
            Set<String> visitedDependencies) {
        Set<ResolvedArtifact> artifacts = []
        resolvedDependency.parents.each { ResolvedDependency parent ->
            artifacts += resolvedDependency.getArtifacts(parent)
        }

        ResolvedArtifact artifact = ++artifacts.iterator()
        if (artifact.id.componentIdentifier.displayName.contains(" ")) {
            // This is a local dependency. It is not in the cache.
            return
        }

        if (!inCache(artifact) || visitedDependencies.contains(getRuleName(artifact))) {
            return
        } else {
            visitedDependencies.add(getRuleName(artifact))
        }

        if (artifact.getExtension().endsWith("aar")) {
            buildFile.append """aar_import(
    name = '${getRuleName(artifact)}',
    aar = '${getRelativePath(artifact)}',
    exports = [
"""
        } else if (artifact.getExtension().endsWith("jar")) {
            buildFile.append """java_import(
    name = '${getRuleName(artifact)}',
    jars = ['${getRelativePath(artifact)}'],
    exports = [
"""
        }
        resolvedDependency.children.collect { child ->
            getRuleName(++child.getArtifacts(resolvedDependency).iterator())
        }.unique().sort().each { String exportName ->
            buildFile.append "        ':${exportName}',\n"
        }
        buildFile.append "    ],\n)\n\n"

        resolvedDependency.children.each { ResolvedDependency child ->
            writeBuildRules(child, buildFile, visitedDependencies)
        }
    }

    private String getRuleName(ResolvedArtifact artifact) {
        String[] pieces = getRelativePath(artifact).split("/")
        return pieces[pieces.length - 1]
    }

    private String getRelativePath(ResolvedArtifact artifact) {
        return cache.get(toExternalDependency(artifact)).split("/", 2)[1]
    }

    private boolean inCache(ResolvedArtifact artifact) {
        try {
            cache.get(toExternalDependency(artifact))
            return true
        } catch (NullPointerException ignored) {
            return false
        }
    }

    private static ExternalDependency toExternalDependency(ResolvedArtifact artifact) {
        return new ExternalDependency(artifact.id.componentIdentifier.displayName, artifact.file)
    }
}
