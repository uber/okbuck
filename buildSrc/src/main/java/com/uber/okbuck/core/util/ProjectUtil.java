package com.uber.okbuck.core.util;

import com.android.build.gradle.AppPlugin;
import com.android.build.gradle.LibraryPlugin;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.model.base.ProjectType;
import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.core.model.base.Target;
import com.uber.okbuck.core.model.base.TargetCache;

import org.gradle.api.Project;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.query.ArtifactResolutionQuery;
import org.gradle.api.artifacts.result.ArtifactResolutionResult;
import org.gradle.api.component.Artifact;
import org.gradle.api.plugins.GroovyPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.plugins.scala.ScalaPlugin;
import org.gradle.jvm.JvmLibrary;
import org.gradle.language.base.artifact.SourcesArtifact;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class ProjectUtil {

    private ProjectUtil() {}

    public static ProjectType getType(Project project) {
        PluginContainer plugins = project.getPlugins();
        if (plugins.hasPlugin(AppPlugin.class)) {
            return ProjectType.ANDROID_APP;
        } else if (plugins.hasPlugin(LibraryPlugin.class)) {
            return ProjectType.ANDROID_LIB;
        } else if (plugins.hasPlugin(GroovyPlugin.class)) {
            return ProjectType.GROOVY_LIB;
        } else if (plugins.hasPlugin(KotlinPluginWrapper.class)) {
            return ProjectType.KOTLIN_LIB;
        } else if (plugins.hasPlugin(ScalaPlugin.class)) {
            return ProjectType.SCALA_LIB;
        } else if (plugins.hasPlugin(JavaPlugin.class)) {
            return ProjectType.JAVA_LIB;
        } else {
            return ProjectType.UNKNOWN;
        }
    }

    public static DependencyCache getDependencyCache(Project project) {
        return getPlugin(project).depCache;
    }

    public static Map<String, Target> getTargets(Project project) {
        return getTargetCache(project).getTargets(project);
    }

    @Nullable
    public static Target getTargetForOutput(Project targetProject, File output) {
        return getTargetCache(targetProject).getTargetForOutput(targetProject, output);
    }

    public static Map<Project, Map<String, Scope>> getScopes(Project project) {
        return getPlugin(project).scopes;
    }

    static OkBuckGradlePlugin getPlugin(Project project) {
        return project.getRootProject().getPlugins().getPlugin(OkBuckGradlePlugin.class);
    }

    private static TargetCache getTargetCache(Project project) {
        return getPlugin(project).targetCache;
    }

    @Nullable
    static String findVersionInClasspath(Project project, String group, String module) {
        return project.getBuildscript()
                .getConfigurations()
                .getByName("classpath")
                .getIncoming()
                .getArtifacts()
                .getArtifacts()
                .stream()
                .flatMap(artifactResult ->
                    artifactResult.getId().getComponentIdentifier() instanceof ModuleComponentIdentifier
                        ? Stream.of((ModuleComponentIdentifier) artifactResult.getId().getComponentIdentifier())
                        : Stream.empty())
                .filter(identifier -> (group.equals(identifier.getGroup()) &&
                        module.equals(identifier.getModule())))
                .findFirst()
                .map(ModuleComponentIdentifier::getVersion)
                .orElse(null);
    }

    /*
     * Copyright (C) 2017 The Android Open Source Project
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *      http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */
    // Copied from AGP 3.1.0 ArtifactDependencyGraph
    static void downloadSources(
            Project project,
            Set<ComponentIdentifier> artifacts) {
        final DependencyHandler dependencies = project.getDependencies();

        try {
            ArtifactResolutionQuery query = dependencies.createArtifactResolutionQuery();
            query.forComponents(artifacts);

            @SuppressWarnings("unchecked")
            Class<? extends Artifact>[] artifactTypesArray =
                    (Class<? extends Artifact>[]) new Class<?>[] {SourcesArtifact.class};
            query.withArtifacts(JvmLibrary.class, artifactTypesArray);
            ArtifactResolutionResult queryResult = query.execute();
            queryResult.getResolvedComponents();
        } catch (Throwable t) {
            System.out.println("Unable to download sources for project " +
                    project.toString() + " with error " + t.toString());
        }
    }
}
