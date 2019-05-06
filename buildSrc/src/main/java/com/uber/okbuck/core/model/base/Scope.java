package com.uber.okbuck.core.model.base;

import com.android.build.api.attributes.VariantAttr;
import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.uber.okbuck.core.annotation.AnnotationProcessorCache;
import com.uber.okbuck.core.annotation.JvmPlugin;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.DependencyFactory;
import com.uber.okbuck.core.dependency.DependencyUtils;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.dependency.VersionlessDependency;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectCache;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.ExternalDependenciesExtension;
import com.uber.okbuck.extension.JetifierExtension;
import com.uber.okbuck.extension.OkBuckExtension;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.specs.Spec;

public class Scope {

  private final Set<String> javaResources;
  private final Set<String> sources;
  @Nullable private final Configuration configuration;
  private final DependencyCache depCache;
  private final Map<String, List<String>> customOptions;
  protected final Project project;

  protected final Set<Target> targetDeps = new HashSet<>();
  protected final Set<ExternalDependency> external = new HashSet<>();

  @Nullable private Set<String> annotationProcessors;

  public final Set<String> getJavaResources() {
    return javaResources;
  }

  public final Set<String> getSources() {
    return sources;
  }

  public Map<String, List<String>> getCustomOptions() {
    return customOptions;
  }

  /** Used to filter out only project dependencies when resolving a configuration. */
  private static final Spec<ComponentIdentifier> PROJECT_FILTER =
      componentIdentifier -> componentIdentifier instanceof ProjectComponentIdentifier;

  /** Used to filter out external & local jar/aar dependencies when resolving a configuration. */
  private static final Spec<ComponentIdentifier> EXTERNAL_DEP_FILTER =
      componentIdentifier -> !(componentIdentifier instanceof ProjectComponentIdentifier);

  Scope(
      Project project,
      @Nullable Configuration configuration,
      Set<File> sourceDirs,
      Set<File> javaResourceDirs,
      Map<String, List<String>> customOptions,
      DependencyCache depCache) {

    this.project = project;
    this.sources = FileUtil.available(project, sourceDirs);
    this.javaResources = FileUtil.available(project, javaResourceDirs);
    this.customOptions = customOptions;
    this.depCache = depCache;
    this.configuration = configuration;

    if (configuration != null) {
      extractConfiguration(configuration);
    }
  }

  protected Scope(
      Project project,
      @Nullable Configuration configuration,
      Set<File> sourceDirs,
      Set<File> javaResourceDirs,
      Map<String, List<String>> customOptions) {
    this(
        project,
        configuration,
        sourceDirs,
        javaResourceDirs,
        customOptions,
        ProjectUtil.getDependencyCache(project));
  }

  public Set<Target> getTargetDeps(boolean firstLevel) {
    if (configuration != null && firstLevel) {
      Set<String> firstLevelProjects =
          configuration
              .getAllDependencies()
              .withType(ProjectDependency.class)
              .stream()
              .map(dependency -> dependency.getDependencyProject().getPath())
              .collect(Collectors.toSet());

      return targetDeps
          .stream()
          .filter(target -> firstLevelProjects.contains(target.getProject().getPath()))
          .collect(Collectors.toSet());
    } else {
      return targetDeps;
    }
  }

  public final Set<Target> getTargetDeps() {
    ExternalDependenciesExtension externalDependenciesExtension =
        ProjectUtil.getExternalDependencyExtension(project);

    return getTargetDeps(externalDependenciesExtension.exportedDepsEnabled());
  }

  public Set<ExternalDependency> getExternalDeps(boolean firstLevel) {
    if (configuration != null && firstLevel) {
      Set<VersionlessDependency> firstLevelDependencies =
          configuration
              .getAllDependencies()
              .withType(org.gradle.api.artifacts.ExternalDependency.class)
              .stream()
              .map(DependencyFactory::fromDependency)
              .flatMap(Collection::stream)
              .collect(Collectors.toSet());

      return external
          .stream()
          .map(depCache::get)
          .filter(
              dependency -> {
                VersionlessDependency vDependency = dependency.getVersionless();

                // Always include local dependencies
                if (vDependency.group().equals(DependencyFactory.LOCAL_GROUP)) {
                  return true;
                }
                return firstLevelDependencies.contains(vDependency);
              })
          .collect(Collectors.toSet());
    } else {
      return external.stream().map(depCache::get).collect(Collectors.toSet());
    }
  }

  public final Set<ExternalDependency> getExternalDeps() {
    OkBuckExtension okBuckExtension = ProjectUtil.getOkBuckExtension(project);
    ExternalDependenciesExtension externalDependenciesExtension =
        okBuckExtension.getExternalDependenciesExtension();

    return getExternalDeps(externalDependenciesExtension.exportedDepsEnabled());
  }

  /**
   * Get the annotation processors string present in the configurations first level dependencies.
   *
   * @return A set containing annotation processor class names.
   */
  public Set<String> getAnnotationProcessors() {
    if (configuration == null) {
      return ImmutableSet.of();
    }

    if (annotationProcessors == null) {
      annotationProcessors =
          Streams.concat(
                  getExternalDeps(true)
                      .stream()
                      .map(depCache::getAnnotationProcessors)
                      .flatMap(Set::stream),
                  getTargetDeps(true)
                      .stream()
                      .map(
                          target -> {
                            OkBuckExtension okBuckExtension = target.getOkbuck();
                            return target.getProp(
                                okBuckExtension.annotationProcessors, ImmutableList.of());
                          })
                      .flatMap(List::stream))
              .filter(StringUtils::isNotEmpty)
              .collect(Collectors.toSet());
    }
    return annotationProcessors;
  }

  /**
   * Check if the annotation processor scope has any auto value extension.
   *
   * @return boolean whether the scope has any auto value extension.
   */
  public boolean hasAutoValueExtensions() {
    return getExternalDeps().stream().anyMatch(depCache::hasAutoValueExtension);
  }

  /**
   * Returns the JvmPlugin for the annotation processor of the scope.
   *
   * @return JvmPlugin
   */
  public JvmPlugin getAnnotationProcessorPlugin() {
    JvmPlugin.Builder jvmPluginBuilder = JvmPlugin.builder();

    Set<ExternalDependency> dependencies = getExternalDeps(true);

    if (dependencies.size() > 1) {
      Optional<ExternalDependency> optionalAutoValue =
          dependencies
              .stream()
              .filter(
                  dep ->
                      dep.getGroup().equals(AnnotationProcessorCache.AUTO_VALUE_GROUP)
                          && dep.getName().equals(AnnotationProcessorCache.AUTO_VALUE_NAME))
              .findAny();
      Preconditions.checkArgument(
          optionalAutoValue.isPresent(),
          "Multiple annotation processor dependencies should have auto value");

      Preconditions.checkNotNull(configuration);
      String configurationName = configuration.getName();

      ExternalDependency autoValue = optionalAutoValue.get();
      String processorUID = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, configurationName);

      jvmPluginBuilder.setPluginUID(processorUID).setPluginDependency(autoValue);

    } else if (dependencies.size() == 1) {
      ExternalDependency dependency = dependencies.stream().findAny().get();
      jvmPluginBuilder.setPluginUID(dependency.getBaseTargetName()).setPluginDependency(dependency);

    } else {
      // Can have only one target dependency
      Optional<Target> target = getTargetDeps(true).stream().findAny();
      String processorUID = target.get().getIdentifier().replace(":", "-");
      jvmPluginBuilder.setPluginUID(processorUID).setPluginTarget(target);
    }
    return jvmPluginBuilder.build();
  }

  private static Set<ResolvedArtifactResult> getArtifacts(
      Configuration configuration, String value, Spec<ComponentIdentifier> filter) {

    return configuration
        .getIncoming()
        .artifactView(
            config -> {
              config.attributes(
                  container ->
                      container.attribute(Attribute.of("artifactType", String.class), value));
              config.componentFilter(filter);
            })
        .getArtifacts()
        .getArtifacts();
  }

  private static ImmutableSet<ResolvedArtifactResult> getArtifacts(
      Configuration configuration,
      Spec<ComponentIdentifier> filter,
      ImmutableList<String> artifactTypes) {

    ImmutableSet.Builder<ResolvedArtifactResult> artifactResultsBuilder = ImmutableSet.builder();

    // We need to individually add these sets to the final set so as to maintain the order.
    // for eg. All aar artifact should come before jar artifacts.
    artifactTypes.forEach(
        artifactType ->
            artifactResultsBuilder.addAll(
                getArtifacts(configuration, artifactType, filter)
                    .stream()
                    .filter(it -> !it.getFile().getName().equals("classes.jar"))
                    .collect(Collectors.toSet())));

    return artifactResultsBuilder.build();
  }

  private void extractConfiguration(Configuration configuration) {
    depCache.addDependencies(configuration.getAllDependencies());

    Set<ResolvedArtifactResult> jarArtifacts =
        getArtifacts(configuration, PROJECT_FILTER, ImmutableList.of("jar"));

    jarArtifacts.forEach(
        artifact -> {
          if (!DependencyUtils.isConsumable(artifact.getFile())) {
            return;
          }

          ProjectComponentIdentifier identifier =
              (ProjectComponentIdentifier) artifact.getId().getComponentIdentifier();
          VariantAttr variantAttr =
              artifact.getVariant().getAttributes().getAttribute(VariantAttr.ATTRIBUTE);
          String variant = variantAttr == null ? null : variantAttr.getName();

          Project identifierProject = project.project(identifier.getProjectPath());
          targetDeps.add(
              ProjectCache.getTargetCache(identifierProject).getTargetForVariant(variant));
        });

    Set<ResolvedArtifactResult> aarOrJarArtifacts =
        getArtifacts(configuration, EXTERNAL_DEP_FILTER, ImmutableList.of("aar", "jar"));

    OkBuckExtension okBuckExtension = ProjectUtil.getOkBuckExtension(project);
    ExternalDependenciesExtension externalDependenciesExtension =
        okBuckExtension.getExternalDependenciesExtension();
    JetifierExtension jetifierExtension = okBuckExtension.getJetifierExtension();

    Set<ResolvedArtifactResult> consumableArtifacts =
        aarOrJarArtifacts
            .stream()
            .filter(artifact -> DependencyUtils.isConsumable(artifact.getFile()))
            .collect(Collectors.toSet());

    Map<ComponentIdentifier, ResolvedArtifactResult> componentIdToSourcesArtifactMap =
        ProjectUtil.downloadSources(project, consumableArtifacts);

    consumableArtifacts.forEach(
        artifact -> {
          ComponentIdentifier identifier = artifact.getId().getComponentIdentifier();
          ResolvedArtifactResult sourcesArtifact = componentIdToSourcesArtifactMap.get(identifier);

          if (identifier instanceof ModuleComponentIdentifier
              && ((ModuleComponentIdentifier) identifier).getVersion().length() > 0) {
            ModuleComponentIdentifier moduleIdentifier = (ModuleComponentIdentifier) identifier;

            ExternalDependency externalDependency =
                DependencyFactory.from(
                    moduleIdentifier.getGroup(),
                    moduleIdentifier.getModule(),
                    moduleIdentifier.getVersion(),
                    artifact.getFile(),
                    sourcesArtifact != null ? sourcesArtifact.getFile() : null,
                    externalDependenciesExtension,
                    jetifierExtension);
            external.add(externalDependency);
          } else {
            String rootProjectPath = project.getRootProject().getProjectDir().getAbsolutePath();
            String artifactPath = artifact.getFile().getAbsolutePath();

            try {
              if (!FilenameUtils.directoryContains(rootProjectPath, artifactPath)
                  && !DependencyUtils.isWhiteListed(artifact.getFile())) {

                throw new IllegalStateException(
                    String.format(
                        "Local dependencies should be under project root. Dependencies "
                            + "outside the project can cause hard to reproduce builds"
                            + ". Please move dependency: %s inside %s",
                        artifact.getFile(), project.getRootProject().getProjectDir()));
              }
              external.add(
                  DependencyFactory.fromLocal(
                      artifact.getFile(),
                      sourcesArtifact != null ? sourcesArtifact.getFile() : null,
                      externalDependenciesExtension,
                      jetifierExtension));

            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        });
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Scope scope = (Scope) o;
    return Objects.equals(javaResources, scope.javaResources)
        && Objects.equals(sources, scope.sources)
        && Objects.equals(configuration, scope.configuration)
        && Objects.equals(customOptions, scope.customOptions)
        && Objects.equals(project, scope.project);
  }

  @Override
  public int hashCode() {

    return Objects.hash(javaResources, sources, configuration, customOptions, project);
  }

  public static Builder builder(Project project) {
    return new Builder(project);
  }

  public static final class Builder {

    private final Project project;

    private Set<File> javaResourceDirs = ImmutableSet.of();
    private Set<File> sourceDirs = ImmutableSet.of();
    @Nullable private Configuration configuration = null;
    private DependencyCache depCache;
    private final Map<String, List<String>> compilerOptions = new LinkedHashMap<>();

    private Builder(Project project) {
      this.project = project;
      depCache = ProjectUtil.getDependencyCache(project);
    }

    public Builder javaResourceDirs(Set<File> javaResourceDirs) {
      this.javaResourceDirs = javaResourceDirs;
      return this;
    }

    public Builder sourceDirs(Set<File> sourceDirs) {
      this.sourceDirs = sourceDirs;
      return this;
    }

    public Builder configuration(@Nullable Configuration configuration) {
      this.configuration = configuration;
      return this;
    }

    public Builder configuration(String configuration) {
      this.configuration = DependencyUtils.useful(configuration, project);
      return this;
    }

    public Builder depCache(DependencyCache depCache) {
      this.depCache = depCache;
      return this;
    }

    public Builder customOptions(String key, List<String> values) {
      List<String> existingOptions =
          compilerOptions.computeIfAbsent(key, key1 -> new ArrayList<>());
      existingOptions.addAll(values);
      return this;
    }

    public Builder customOptions(Map<String, List<String>> options) {
      options
          .keySet()
          .forEach(
              key -> {
                List<String> existingOptions =
                    compilerOptions.computeIfAbsent(key, key1 -> new ArrayList<>());
                existingOptions.addAll(options.get(key));
              });
      return this;
    }

    public Scope build() {
      Configuration useful = DependencyUtils.useful(configuration);
      String key = useful != null ? useful.getName() : "--none--";

      return ProjectCache.getScopeCache(project)
          .computeIfAbsent(
              key,
              t ->
                  new Scope(
                      project, useful, sourceDirs, javaResourceDirs, compilerOptions, depCache));
    }
  }
}
