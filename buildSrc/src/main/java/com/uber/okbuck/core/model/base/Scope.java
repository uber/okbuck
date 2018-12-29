package com.uber.okbuck.core.model.base;

import static com.uber.okbuck.core.dependency.BaseExternalDependency.AAR;
import static com.uber.okbuck.core.dependency.BaseExternalDependency.JAR;

import com.android.build.api.attributes.VariantAttr;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.uber.okbuck.core.annotation.AnnotationProcessorCache;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.specs.Spec;

public class Scope {

  private static final String EMPTY_GROUP = "----empty----";

  private final Set<String> javaResources;
  private final Set<String> sources;
  @Nullable private final Configuration configuration;
  private final DependencyCache depCache;
  private final Map<Builder.COMPILER, List<String>> compilerOptions;
  protected final Project project;

  private final Set<Target> targetDeps = new HashSet<>();
  private final Set<ExternalDependency> external = new HashSet<>();

  @Nullable private Set<String> annotationProcessors;

  public final Set<String> getJavaResources() {
    return javaResources;
  }

  public final Set<String> getSources() {
    return sources;
  }

  public final Set<Target> getTargetDeps() {
    return targetDeps;
  }

  public Map<Builder.COMPILER, List<String>> getCompilerOptions() {
    return compilerOptions;
  }

  public final Set<ExternalDependency> getExternal() {
    return external;
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
      Map<Builder.COMPILER, List<String>> compilerOptions,
      DependencyCache depCache) {

    this.project = project;
    this.sources = FileUtil.available(project, sourceDirs);
    this.javaResources = FileUtil.available(project, javaResourceDirs);
    this.compilerOptions = compilerOptions;
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
      Map<Builder.COMPILER, List<String>> compilerOptions) {
    this(
        project,
        configuration,
        sourceDirs,
        javaResourceDirs,
        compilerOptions,
        ProjectUtil.getDependencyCache(project));
  }

  public Set<ExternalDependency> getExternalDeps() {
    return external.stream().map(depCache::get).collect(Collectors.toSet());
  }

  public Set<ExternalDependency> getExternalJarDeps() {
    return external
        .stream()
        .map(depCache::get)
        .filter(dependency -> dependency.getPackaging().equals(JAR))
        .collect(Collectors.toSet());
  }

  public Set<ExternalDependency> getExternalAarDeps() {
    return external
        .stream()
        .map(depCache::get)
        .filter(dependency -> dependency.getPackaging().equals(AAR))
        .collect(Collectors.toSet());
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
      Set<VersionlessDependency> firstLevelDependencies =
          configuration
              .getAllDependencies()
              .stream()
              .map(
                  dependency -> {
                    String group =
                        dependency.getGroup() == null ? EMPTY_GROUP : dependency.getGroup();
                    return VersionlessDependency.builder()
                        .setGroup(group)
                        .setName(dependency.getName())
                        .build();
                  })
              .collect(Collectors.toSet());

      annotationProcessors =
          Streams.concat(
                  external
                      .stream()
                      .filter(
                          dependency ->
                              firstLevelDependencies.contains(dependency.getVersionless()))
                      .map(depCache::getAnnotationProcessors)
                      .flatMap(Set::stream),
                  targetDeps
                      .stream()
                      .filter(
                          target -> {
                            VersionlessDependency versionless =
                                VersionlessDependency.builder()
                                    .setGroup((String) target.getProject().getGroup())
                                    .setName(target.getProject().getName())
                                    .build();
                            return firstLevelDependencies.contains(versionless);
                          })
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
    return external.stream().anyMatch(depCache::hasAutoValueExtensions);
  }

  /**
   * Returns the UID for the annotation processors of the scope.
   *
   * @return String UID
   */
  public String getAnnotationProcessorsUID() {
    Preconditions.checkNotNull(configuration);
    DependencySet dependencies = configuration.getAllDependencies();
    String processorsUID =
        dependencies
            .stream()
            .map(
                dep -> {
                  if (dep.getVersion() == null || dep.getVersion().length() == 0) {
                    return String.format("%s-%s", dep.getGroup(), dep.getName());
                  } else {
                    return String.format(
                        "%s-%s-%s", dep.getGroup(), dep.getName(), dep.getVersion());
                  }
                })
            .filter(name -> name.length() > 0)
            .sorted()
            .collect(Collectors.joining("-"));

    if (dependencies.size() > 1) {
      // Use md5 hash when there are multiple dependencies along with auto value.
      // Multiple dependencies will only happen when auto value extensions are present.

      Optional<String> autoValueUID =
          dependencies
              .stream()
              .filter(
                  dep ->
                      dep.getGroup() != null
                          && dep.getGroup().equals(AnnotationProcessorCache.AUTO_VALUE_GROUP)
                          && dep.getName().equals(AnnotationProcessorCache.AUTO_VALUE_NAME))
              .map(
                  dep -> String.format("%s-%s-%s", dep.getGroup(), dep.getName(), dep.getVersion()))
              .findAny();

      Preconditions.checkArgument(
          autoValueUID.isPresent(),
          "Multiple annotation processor dependencies should have auto value");

      String md5Hash = DigestUtils.md5Hex(processorsUID);
      return String.format("%s__%s", autoValueUID.get(), md5Hash);
    } else {
      return processorsUID;
    }
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
    Set<ComponentIdentifier> artifactIds = new HashSet<>();

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

    aarOrJarArtifacts.forEach(
        artifact -> {
          if (!DependencyUtils.isConsumable(artifact.getFile())) {
            return;
          }

          ComponentIdentifier identifier = artifact.getId().getComponentIdentifier();
          artifactIds.add(identifier);

          if (identifier instanceof ModuleComponentIdentifier
              && ((ModuleComponentIdentifier) identifier).getVersion().length() > 0) {
            ModuleComponentIdentifier moduleIdentifier = (ModuleComponentIdentifier) identifier;

            ExternalDependency externalDependency =
                DependencyFactory.from(
                    moduleIdentifier.getGroup(),
                    moduleIdentifier.getModule(),
                    moduleIdentifier.getVersion(),
                    artifact.getFile(),
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
                      artifact.getFile(), externalDependenciesExtension, jetifierExtension));

            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        });

    if (ProjectUtil.getOkBuckExtension(project).getIntellijExtension().sources) {
      ProjectUtil.downloadSources(project, artifactIds);
    }
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
        && Objects.equals(compilerOptions, scope.compilerOptions)
        && Objects.equals(project, scope.project);
  }

  @Override
  public int hashCode() {

    return Objects.hash(javaResources, sources, configuration, compilerOptions, project);
  }

  public static Builder builder(Project project) {
    return new Builder(project);
  }

  public static final class Builder {

    public enum COMPILER {
      JAVA,
      KOTLIN,
      SCALA
    }

    private final Project project;

    private Set<File> javaResourceDirs = ImmutableSet.of();
    private Set<File> sourceDirs = ImmutableSet.of();
    @Nullable private Configuration configuration = null;
    private DependencyCache depCache;
    private final Map<COMPILER, List<String>> compilerOptions = new LinkedHashMap<>();

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

    public Builder compilerOptions(COMPILER compiler, List<String> options) {
      List<String> existingOptions =
          compilerOptions.computeIfAbsent(compiler, compiler1 -> new ArrayList<>());
      existingOptions.addAll(options);
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
