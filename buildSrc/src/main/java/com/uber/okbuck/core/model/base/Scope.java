package com.uber.okbuck.core.model.base;

import com.android.build.api.attributes.VariantAttr;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.DependencyUtils;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.dependency.ExternalDependency.VersionlessDependency;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.OkBuckExtension;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.specs.Spec;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Scope {
    public static final String EMPTY_GROUP = "----empty----";

    private final Set<String> javaResources;
    private final Set<String> sources;
    private final Configuration configuration;

    private List<String> jvmArgs;
    private DependencyCache depCache;

    protected final Project project;

    private final Set<Target> targetDeps = new HashSet<>();
    private final Set<ExternalDependency> external = new HashSet<>();

    public final Set<String> getJavaResources() {
        return javaResources;
    }

    public final Set<String> getSources() {
        return sources;
    }

    public final Set<Target> getTargetDeps() {
        return targetDeps;
    }

    public List<String> getJvmArgs() {
        return jvmArgs;
    }

    public void setJvmArgs(List<String> jvmArgs) {
        this.jvmArgs = jvmArgs;
    }

    public final Set<ExternalDependency> getExternal() {
        return external;
    }

    /**
     * Used to filter out only project dependencies when resolving a configuration.
     */
    private static final Spec<ComponentIdentifier> PROJECT_FILTER =
            componentIdentifier -> componentIdentifier instanceof ProjectComponentIdentifier;

    /**
     * Used to filter out external & local jar/aar dependencies when resolving a configuration.
     */
    private static final Spec<ComponentIdentifier> EXTERNAL_DEP_FILTER =
            componentIdentifier -> !(componentIdentifier instanceof ProjectComponentIdentifier);

    protected Scope(
            Project project,
            Configuration configuration,
            Set<File> sourceDirs,
            Set<File> javaResourceDirs,
            List<String> jvmArguments,
            DependencyCache depCache) {

        this.project = project;
        this.sources = FileUtil.available(project, sourceDirs);
        this.javaResources = FileUtil.available(project, javaResourceDirs);
        this.jvmArgs = jvmArguments;
        this.depCache = depCache;
        this.configuration = configuration;

        if (configuration != null) {
            extractConfiguration(configuration);
        }
    }

    protected Scope(
            Project project,
            Configuration configuration,
            Set<File> sourceDirs,
            Set<File> javaResourceDirs,
            List<String> jvmArguments) {
        this(project, configuration, sourceDirs, javaResourceDirs, jvmArguments,
                ProjectUtil.getDependencyCache(project));
    }

    public static Scope from(
            Project project,
            String configuration,
            Set<File> sourceDirs,
            Set<File> javaResourceDirs,
            List<String> jvmArguments,
            DependencyCache depCache) {
        Configuration useful = DependencyUtils.useful(project, configuration);
        return from(project, useful, sourceDirs, javaResourceDirs, jvmArguments, depCache);
    }

    public static Scope from(
            Project project,
            String configuration,
            Set<File> sourceDirs,
            Set<File> javaResourceDirs,
            List<String> jvmArguments) {
        return Scope.from(project, configuration, sourceDirs, javaResourceDirs, jvmArguments,
                ProjectUtil.getDependencyCache(project));
    }

    public static Scope from(
            Project project,
            String configuration,
            Set<File> sourceDirs,
            Set<File> javaResourceDirs) {
        return Scope.from(project, configuration, sourceDirs, javaResourceDirs, ImmutableList.of(),
                ProjectUtil.getDependencyCache(project));
    }

    public static Scope from(Project project, String configuration, Set<File> sourceDirs) {
        return Scope.from(project, configuration, sourceDirs, ImmutableSet.of(), ImmutableList.of(),
                ProjectUtil.getDependencyCache(project));
    }

    public static Scope from(Project project, String configuration) {
        return Scope.from(project, configuration, ImmutableSet.of(), ImmutableSet.of(),
                ImmutableList.of(), ProjectUtil.getDependencyCache(project));
    }

    public static Scope from(
            Project project,
            Configuration configuration,
            Set<File> sourceDirs,
            Set<File> javaResourceDirs,
            List<String> jvmArguments,
            DependencyCache depCache) {
        Configuration useful = DependencyUtils.useful(configuration);
        String key = useful != null ? useful.getName() : "--none--";

        return ProjectUtil
                .getScopes(project)
                .computeIfAbsent(project, t -> new ConcurrentHashMap<>())
                .computeIfAbsent(key, t ->
                        new Scope(project, useful, sourceDirs, javaResourceDirs,
                                jvmArguments, depCache)
                );
    }

    public static Scope from(
            Project project,
            Configuration configuration,
            Set<File> sourceDirs,
            Set<File> javaResourceDirs,
            List<String> jvmArguments) {
        return Scope.from(project, configuration, sourceDirs, javaResourceDirs, jvmArguments,
                ProjectUtil.getDependencyCache(project));
    }

    public static Scope from(
            Project project,
            Configuration configuration,
            Set<File> sourceDirs,
            Set<File> javaResourceDirs) {
        return Scope.from(project, configuration, sourceDirs, javaResourceDirs, ImmutableList.of(),
                ProjectUtil.getDependencyCache(project));
    }

    public static Scope from(Project project, Configuration configuration, Set<File> sourceDirs) {
        return Scope.from(project, configuration, sourceDirs, ImmutableSet.of(), ImmutableList.of(),
                ProjectUtil.getDependencyCache(project));
    }

    public static Scope from(Project project, Configuration configuration) {
        return Scope.from(project, configuration, ImmutableSet.of(), ImmutableSet.of(),
                ImmutableList.of(), ProjectUtil.getDependencyCache(project));
    }

    public Set<String> getExternalDeps() {
        return external
                .stream()
                .map(depCache::get)
                .collect(Collectors.toSet());
    }

    public Set<String> getPackagedLintJars() {
        return external
                .stream()
                .filter(i -> i.depFile.getName().endsWith("aar"))
                .map(depCache::getLintJar)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
    }

    public Set<String> getAnnotationProcessors() {
        Set<ExternalDependency.VersionlessDependency> firstLevelDependencies = configuration
                .getAllDependencies()
                .stream()
                .map(i -> new ExternalDependency.VersionlessDependency(
                        i.getGroup() == null ? EMPTY_GROUP : i.getGroup(),
                        i.getName())
                )
                .collect(Collectors.toSet());

        return Streams.concat(
                external
                        .stream()
                        .filter(i -> firstLevelDependencies.contains(i.versionless))
                        .map(depCache::getAnnotationProcessors)
                        .flatMap(Set::stream),
                targetDeps
                        .stream()
                        .filter(i -> {
                            VersionlessDependency versionless =
                                    new ExternalDependency.VersionlessDependency(
                                            (String) i.getProject().getGroup(),
                                            i.getProject().getName()
                                    );
                            return firstLevelDependencies.contains(versionless);
                        })
                        .map(target -> {
                            OkBuckExtension okBuckExtension = ProjectUtil.getOkBuckExtension(project);
                            return target.getProp(okBuckExtension.annotationProcessors, ImmutableList.of());
                        })
                        .flatMap(List::stream))
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
    }

    private static Set<ResolvedArtifactResult> getArtifacts(
            Configuration configuration,
            final String value,
            final Spec<ComponentIdentifier> filter) {

        return configuration.getIncoming().artifactView(config -> {
            config.attributes(container ->
                    container.attribute(Attribute.of("artifactType", String.class), value));
            config.componentFilter(filter);
        }).getArtifacts().getArtifacts();
    }

    private static Set<ResolvedArtifactResult> getArtifacts(
            final Configuration configuration,
            final Spec<ComponentIdentifier> filter,
            ImmutableList<String> artifactTypes) {

        ImmutableSet.Builder<ResolvedArtifactResult> artifactResultsBuilder =
                ImmutableSet.builder();

        // We need to individually add these sets to the final set so as to maintain the order.
        // for eg. All aar artifact should come before jar artifacts.
        artifactTypes.forEach(artifactType -> artifactResultsBuilder.addAll(
                getArtifacts(configuration, artifactType, filter)
                        .stream()
                        .filter(it -> !it.getFile().getName().equals("classes.jar"))
                        .collect(Collectors.toSet())
                ));

        return artifactResultsBuilder.build();
    }

    private void extractConfiguration(Configuration configuration) {
        Set<ComponentIdentifier> artifactIds = new HashSet<>();

        Set<ResolvedArtifactResult> artifacts =
                getArtifacts(configuration, PROJECT_FILTER, ImmutableList.of("jar"));

        artifacts.forEach(artifact -> {
            if (!DependencyUtils.isConsumable(artifact.getFile())) {
                return;
            }

            ProjectComponentIdentifier identifier = (ProjectComponentIdentifier) artifact.getId()
                    .getComponentIdentifier();
            VariantAttr variantAttr = artifact.getVariant().getAttributes()
                    .getAttribute(VariantAttr.ATTRIBUTE);
            String variant = variantAttr == null ? null : variantAttr.getName();

            targetDeps.add(ProjectUtil.getTargetForVariant(
                    project.project(identifier.getProjectPath()), variant));
        });

        artifacts = getArtifacts(
                configuration,
                EXTERNAL_DEP_FILTER,
                ImmutableList.of("aar", "jar"));

        artifacts.forEach( artifact -> {
            if (!DependencyUtils.isConsumable(artifact.getFile())) {
                return;
            }

            ComponentIdentifier identifier = artifact.getId().getComponentIdentifier();
            artifactIds.add(identifier);

            if (identifier instanceof ModuleComponentIdentifier &&
                    ((ModuleComponentIdentifier) identifier).getVersion().length() > 0) {
                ModuleComponentIdentifier moduleIdentifier = (ModuleComponentIdentifier) identifier;
                ExternalDependency externalDependency = new ExternalDependency(
                        moduleIdentifier.getGroup(),
                        moduleIdentifier.getModule(),
                        moduleIdentifier.getVersion(),
                        artifact.getFile()
                );
                external.add(externalDependency);
            } else {
                String rootProjectPath = project.getRootProject().getProjectDir().getAbsolutePath();
                String artifactPath = artifact.getFile().getAbsolutePath();

                try {
                    if (!FilenameUtils.directoryContains(rootProjectPath, artifactPath)
                            && !DependencyUtils.isWhiteListed(artifact.getFile())) {

                        throw new IllegalStateException(String.format(
                                "Local dependencies should be under project root. Dependencies "
                                        + "outside the project can cause hard to reproduce builds"
                                        + ". Please move dependency: %s inside %s",
                                artifact.getFile(),
                                project.getRootProject().getProjectDir()));
                    }
                    external.add(ExternalDependency.fromLocal(artifact.getFile()));

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
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        Scope scope = (Scope) o;
        return Objects.equals(javaResources, scope.javaResources) &&
                Objects.equals(sources, scope.sources) &&
                Objects.equals(jvmArgs, scope.jvmArgs) &&
                Objects.equals(project, scope.project);
    }

    @Override
    public int hashCode() {
        return Objects.hash(javaResources, sources, jvmArgs, project);
    }
}
