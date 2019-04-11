package com.uber.okbuck.core.util;

import static com.uber.okbuck.core.dependency.BaseExternalDependency.AAR;
import static com.uber.okbuck.core.dependency.BaseExternalDependency.JAR;

import com.android.build.gradle.AppPlugin;
import com.android.build.gradle.LibraryPlugin;
import com.google.common.collect.ImmutableList;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.annotation.AnnotationProcessorCache;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.DependencyUtils;
import com.uber.okbuck.core.manager.DependencyManager;
import com.uber.okbuck.core.manager.GroovyManager;
import com.uber.okbuck.core.manager.KotlinManager;
import com.uber.okbuck.core.manager.LintManager;
import com.uber.okbuck.core.manager.ScalaManager;
import com.uber.okbuck.core.manager.TransformManager;
import com.uber.okbuck.core.model.base.ProjectType;
import com.uber.okbuck.extension.ExternalDependenciesExtension;
import com.uber.okbuck.extension.OkBuckExtension;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.result.ComponentArtifactsResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.component.Artifact;
import org.gradle.api.plugins.GroovyPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.plugins.scala.ScalaPlugin;
import org.gradle.jvm.JvmLibrary;
import org.gradle.language.base.artifact.SourcesArtifact;
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper;

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

  public static AnnotationProcessorCache getAnnotationProcessorCache(Project project) {
    return getPlugin(project).annotationProcessorCache;
  }

  public static DependencyManager getDependencyManager(Project project) {
    return getPlugin(project).dependencyManager;
  }

  public static LintManager getLintManager(Project project) {
    return getPlugin(project).lintManager;
  }

  public static KotlinManager getKotlinManager(Project project) {
    return getPlugin(project).kotlinManager;
  }

  public static ScalaManager getScalaManager(Project project) {
    return getPlugin(project).scalaManager;
  }

  public static GroovyManager getGroovyManager(Project project) {
    return getPlugin(project).groovyManager;
  }

  public static TransformManager getTransformManager(Project project) {
    return getPlugin(project).transformManager;
  }

  public static OkBuckGradlePlugin getPlugin(Project project) {
    return project.getRootProject().getPlugins().getPlugin(OkBuckGradlePlugin.class);
  }

  public static OkBuckExtension getOkBuckExtension(Project project) {
    return (OkBuckExtension)
        project.getRootProject().getExtensions().getByName(OkBuckGradlePlugin.OKBUCK);
  }

  public static ExternalDependenciesExtension getExternalDependencyExtension(Project project) {
    OkBuckExtension okBuckExtension =
        (OkBuckExtension)
            project.getRootProject().getExtensions().getByName(OkBuckGradlePlugin.OKBUCK);
    return okBuckExtension.getExternalDependenciesExtension();
  }

  @Nullable
  public static String findVersionInClasspath(Project project, String group, String module) {
    return project
        .getBuildscript()
        .getConfigurations()
        .getByName("classpath")
        .getIncoming()
        .getArtifacts()
        .getArtifacts()
        .stream()
        .flatMap(
            artifactResult ->
                artifactResult.getId().getComponentIdentifier() instanceof ModuleComponentIdentifier
                    ? Stream.of(
                        (ModuleComponentIdentifier) artifactResult.getId().getComponentIdentifier())
                    : Stream.empty())
        .filter(
            identifier ->
                (group.equals(identifier.getGroup()) && module.equals(identifier.getModule())))
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
  public static Map<ComponentIdentifier, ResolvedArtifactResult> downloadSources(
      Project project, Set<ResolvedArtifactResult> artifacts) {

    // Download sources if enabled via intellij extension
    if (!ProjectUtil.getOkBuckExtension(project).getIntellijExtension().downloadSources()) {
      return new HashMap<>();
    }

    DependencyHandler dependencies = project.getDependencies();

    try {
      Set<ComponentIdentifier> identifiers =
          artifacts
              .stream()
              .filter(artifact -> canHaveSources(artifact.getFile()))
              .map(artifact -> artifact.getId().getComponentIdentifier())
              .collect(Collectors.toSet());

      @SuppressWarnings("unchecked")
      Class<? extends Artifact>[] artifactTypesArray =
          (Class<? extends Artifact>[]) new Class<?>[] {SourcesArtifact.class};

      Set<ComponentArtifactsResult> results =
          dependencies
              .createArtifactResolutionQuery()
              .forComponents(identifiers)
              .withArtifacts(JvmLibrary.class, artifactTypesArray)
              .execute()
              .getResolvedComponents();

      // Only has one type.
      Class<? extends Artifact> type = artifactTypesArray[0];

      return results
          .stream()
          .map(artifactsResult -> artifactsResult.getArtifacts(type))
          .flatMap(Set::stream)
          .filter(artifactResult -> artifactResult instanceof ResolvedArtifactResult)
          .map(artifactResult -> (ResolvedArtifactResult) artifactResult)
          .filter(artifactResult -> FileUtil.isZipFile(artifactResult.getFile()))
          .collect(
              Collectors.toMap(
                  resolvedArtifact -> resolvedArtifact.getId().getComponentIdentifier(),
                  resolvedArtifact -> resolvedArtifact));

    } catch (Throwable t) {
      System.out.println(
          "Unable to download sources for project "
              + project.toString()
              + " with error "
              + t.toString());

      return new HashMap<>();
    }
  }

  public static boolean canHaveSources(File dependencyFile) {
    return !DependencyUtils.isWhiteListed(dependencyFile)
        && ImmutableList.of(JAR, AAR)
            .contains(FilenameUtils.getExtension(dependencyFile.getName()));
  }
}
