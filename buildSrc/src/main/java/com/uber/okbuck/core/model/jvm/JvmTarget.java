package com.uber.okbuck.core.model.jvm;

import static com.uber.okbuck.core.dependency.ExternalDependency.filterAar;
import static com.uber.okbuck.core.dependency.ExternalDependency.filterJar;

import com.android.builder.model.LintOptions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.Var;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.jvm.JvmBuckRuleComposer;
import com.uber.okbuck.core.annotation.AnnotationProcessorCache;
import com.uber.okbuck.core.annotation.JvmPlugin;
import com.uber.okbuck.core.dependency.DependencyFactory;
import com.uber.okbuck.core.dependency.DependencyUtils;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.dependency.VersionlessDependency;
import com.uber.okbuck.core.manager.KotlinManager;
import com.uber.okbuck.core.manager.LintManager;
import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.core.model.base.SourceSetType;
import com.uber.okbuck.core.model.base.Target;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.ExternalDependenciesExtension;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.ApplicationPluginConvention;
import org.gradle.api.plugins.BasePluginConvention;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.kotlin.allopen.gradle.AllOpenKotlinGradleSubplugin;
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions;
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper;
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin;
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption;
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile;

public class JvmTarget extends Target {

  public static final String MAIN = "main";

  protected static final String JAVA_COMPILER_EXTRA_ARGUMENTS = "extra_arguments";
  protected static final String KOTLIN_COMPILER_EXTRA_ARGUMENTS = "extra_kotlinc_arguments";

  private static final String INTEGRATION_TEST_SOURCE_SET_NAME = "integrationTest";
  private static final String INTEGRATION_TEST_TASK_NAME = "integrationTest";
  private static final String INTEGRATION_TEST_ANNOTATION_PROCESSOR_CONFIGURATION_NAME = "integrationTestAnnotationProcessor";
  private static final String INTEGRATION_TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME = "integrationTestRuntimeClasspath";
  private static final String INTEGRATION_TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME = "integrationTestCompileClasspath";
  private static final String COMPILE_INTEGRATION_TEST_JAVA_TASK_NAME = "compileIntegrationTestJava";

  private final String aptConfigurationName;
  private final String testAptConfigurationName;
  private final String integrationTestAptConfigurationName;
  private final SourceSetContainer sourceSets;
  protected final boolean isKotlin;

  @Nullable
  private final AbstractCompile fakeCompile;

  public JvmTarget(Project project, String name) {
    this(
        project,
        name,
        JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME,
        JavaPlugin.TEST_ANNOTATION_PROCESSOR_CONFIGURATION_NAME,
        INTEGRATION_TEST_ANNOTATION_PROCESSOR_CONFIGURATION_NAME);
  }

  public JvmTarget(
      Project project,
      String name,
      String aptConfigurationName,
      String testAptConfigurationName,
      String integrationTestAptConfigurationName) {
    super(project, name);
    this.aptConfigurationName = aptConfigurationName;
    this.testAptConfigurationName = testAptConfigurationName;
    this.integrationTestAptConfigurationName = integrationTestAptConfigurationName;
    sourceSets = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
    isKotlin =
        project.getPlugins().stream().anyMatch(plugin -> plugin instanceof KotlinBasePluginWrapper);

    Optional<Task> compileTask =
        project.getTasks().stream().filter(it -> it instanceof AbstractCompile).findFirst();
    fakeCompile = (AbstractCompile) compileTask.orElse(null);
  }

  /**
   * The test options
   *
   * @return The test options
   */
  public TestOptions getTestOptions() {
    Test testTask = (Test) getProject().getTasks().getByName(JavaPlugin.TEST_TASK_NAME);
    Map<String, Object> env = testTask.getEnvironment();
    env.keySet().removeAll(System.getenv().keySet());
    return new TestOptions(testTask.getAllJvmArgs(), testTask.getEnvironment());
  }

  public TestOptions getIntegrationTestOptions() {
    Test testTask = (Test) getProject().getTasks().getByName(INTEGRATION_TEST_TASK_NAME);
    Map<String, Object> env = testTask.getEnvironment();
    env.keySet().removeAll(System.getenv().keySet());
    return new TestOptions(testTask.getAllJvmArgs(), testTask.getEnvironment());
  }

  /**
   * Apt Scopes
   */
  public List<Scope> getAptScopes() {
    AnnotationProcessorCache apCache = ProjectUtil.getAnnotationProcessorCache(getProject());
    return apCache.getAnnotationProcessorScopes(getProject(), aptConfigurationName);
  }

  /**
   * Test Apt Scopes
   */
  public List<Scope> getTestAptScopes() {
    AnnotationProcessorCache apCache = ProjectUtil.getAnnotationProcessorCache(getProject());
    return apCache.getAnnotationProcessorScopes(getProject(), testAptConfigurationName);
  }

  /**
   * Integration Test Apt Scopes
   */
  public List<Scope> getIntegrationTestAptScopes() {
    AnnotationProcessorCache apCache = ProjectUtil.getAnnotationProcessorCache(getProject());
    return apCache.getAnnotationProcessorScopes(getProject(), integrationTestAptConfigurationName);
  }

  /**
   * Apt Scope Used to get the annotation processor deps of the target.
   */
  public Scope getApt() {
    return getAptScopeForConfiguration(aptConfigurationName);
  }

  /**
   * Test Apt Scope
   */
  public Scope getTestApt() {
    return getAptScopeForConfiguration(testAptConfigurationName);
  }

  /**
   * Integration Test Apt Scope
   */
  public Scope getIntegrationTestApt() {
    return getAptScopeForConfiguration(integrationTestAptConfigurationName);
  }

  protected Scope getAptScopeForConfiguration(String configurationName) {
    // If using annotation processor plugin, return an empty scope if there are no annotation
    // processors so no need to have any specified in the annotation processor deps list.
    if (!getOkbuck().legacyAnnotationProcessorSupport || !ProjectUtil
        .getAnnotationProcessorCache(getProject())
        .hasEmptyAnnotationProcessors(getProject(), configurationName)) {
      return Scope.builder(getProject()).build();
    }
    return Scope.builder(getProject()).configuration(configurationName).build();
  }

  protected Scope getAptScopeForConfiguration(Configuration configuration) {
    return getAptScopeForConfiguration(configuration.getName());
  }

  /**
   * Provided Scope
   */
  public Scope getProvided() {
    return Scope.builder(getProject())
        .configuration(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)
        .build();
  }

  /**
   * Test Provided Scope
   */
  public Scope getTestProvided() {
    return Scope.builder(getProject())
        .configuration(JavaPlugin.TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME)
        .build();
  }

  /**
   * Integration Test Provided Scope
   */
  public Scope getIntegrationTestProvided() {
    return Scope.builder(getProject())
        .configuration(INTEGRATION_TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME)
        .build();
  }

  /**
   * api external deps
   */
  public Set<ExternalDependency> getApiExternalDeps() {
    Configuration apiConfiguration = getApiConfiguration();

    if (apiConfiguration != null) {
      Set<VersionlessDependency> versionlessApiDependencies =
          apiConfiguration
              .getAllDependencies()
              .withType(org.gradle.api.artifacts.ExternalDependency.class)
              .stream()
              .map(DependencyFactory::fromDependency)
              .flatMap(Collection::stream)
              .collect(Collectors.toSet());

      return getMain()
          .getExternalDeps()
          .stream()
          .filter(dependency -> versionlessApiDependencies.contains(dependency.getVersionless()))
          .collect(Collectors.toSet());
    } else {
      return ImmutableSet.of();
    }
  }

  /**
   * api target deps
   */
  public Set<Target> getApiTargetDeps() {
    Configuration apiConfiguration = getApiConfiguration();

    if (apiConfiguration != null) {
      Set<String> projectApiDependencies =
          apiConfiguration
              .getAllDependencies()
              .withType(ProjectDependency.class)
              .stream()
              .map(p -> p.getDependencyProject().getPath())
              .collect(Collectors.toSet());

      return getMain()
          .getTargetDeps()
          .stream()
          .filter(dependency -> projectApiDependencies.contains(dependency.getProject().getPath()))
          .collect(Collectors.toSet());
    } else {
      return ImmutableSet.of();
    }
  }

  @Nullable
  private Configuration getApiConfiguration() {
    ExternalDependenciesExtension extension =
        ProjectUtil.getExternalDependencyExtension(getProject());

    if (extension.exportedDepsEnabled()) {
      @Var
      Configuration apiConfiguration =
          DependencyUtils.getConfiguration(
              JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME, getProject());

      if (apiConfiguration == null) {
        apiConfiguration =
            DependencyUtils.getConfiguration(JavaPlugin.API_CONFIGURATION_NAME, getProject());
      }

      return apiConfiguration;
    } else {
      return null;
    }
  }

  /**
   * Lint Scope
   */
  public Scope getLint() {
    LintManager manager = ProjectUtil.getLintManager(getProject());
    return Scope.builder(getProject())
        .configuration(OkBuckGradlePlugin.BUCK_LINT)
        .depCache(manager.getLintDepsCache())
        .build();
  }

  @Nullable
  public LintOptions getLintOptions() {
    return null;
  }

  /**
   * List of annotation processor classes. If annotation processor plugin is enabled returns the
   * annotation processor's UID.
   */
  public Set<JvmPlugin> getApPlugins() {
    return getAptScopes()
        .stream()
        .filter(scope -> !scope.getAnnotationProcessors().isEmpty())
        .map(Scope::getAnnotationProcessorPlugin)
        .collect(Collectors.toSet());
  }

  /**
   * List of test annotation processor classes. If annotation processor plugin is enabled returns
   * the annotation processor's UID.
   */
  public Set<JvmPlugin> getTestApPlugins() {
    return getTestAptScopes()
        .stream()
        .filter(scope -> !scope.getAnnotationProcessors().isEmpty())
        .map(Scope::getAnnotationProcessorPlugin)
        .collect(Collectors.toSet());
  }

  /**
   * List of integration test annotation processor classes. If annotation processor plugin is
   * enabled returns the annotation processor's UID.
   */
  public Set<JvmPlugin> getIntegrationTestApPlugins() {
    return getIntegrationTestAptScopes()
        .stream()
        .filter(scope -> !scope.getAnnotationProcessors().isEmpty())
        .map(Scope::getAnnotationProcessorPlugin)
        .collect(Collectors.toSet());
  }

  public Scope getMain() {
    JavaCompile compileJavaTask =
        (JavaCompile) getProject().getTasks().getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME);
    return Scope.builder(getProject())
        .configuration(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        .sourceDirs(getMainSrcDirs())
        .javaResourceDirs(getMainJavaResourceDirs())
        .customOptions(
            JAVA_COMPILER_EXTRA_ARGUMENTS, compileJavaTask.getOptions().getCompilerArgs())
        .customOptions(KOTLIN_COMPILER_EXTRA_ARGUMENTS, getKotlinCompilerOptions())
        .customOptions(getKotlinFriendPaths(false))
        .build();
  }

  public Scope getTest() {
    JavaCompile testCompileJavaTask =
        (JavaCompile) getProject().getTasks().getByName(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME);
    return Scope.builder(getProject())
        .configuration(JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        .sourceDirs(getTestSrcDirs())
        .javaResourceDirs(getTestJavaResourceDirs())
        .customOptions(
            JAVA_COMPILER_EXTRA_ARGUMENTS, testCompileJavaTask.getOptions().getCompilerArgs())
        .customOptions(KOTLIN_COMPILER_EXTRA_ARGUMENTS, getKotlinCompilerOptions())
        .customOptions(getKotlinFriendPaths(true))
        .build();
  }

  public Scope getIntegrationTest() {
    @Var
    JavaCompile integrationTestCompileJavaTask;
    try {
      // This task might not exist to the module
      integrationTestCompileJavaTask =
          (JavaCompile) getProject().getTasks().getByName(COMPILE_INTEGRATION_TEST_JAVA_TASK_NAME);
    } catch (UnknownTaskException e) {
      integrationTestCompileJavaTask =
          (JavaCompile) getProject().getTasks().getByName(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME);
    }
    return Scope.builder(getProject())
        .configuration(INTEGRATION_TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        .sourceDirs(getIntegrationTestSrcDirs())
        .javaResourceDirs(getIntegrationTestJavaResourceDirs())
        .customOptions(
            JAVA_COMPILER_EXTRA_ARGUMENTS,
            integrationTestCompileJavaTask.getOptions().getCompilerArgs())
        .customOptions(KOTLIN_COMPILER_EXTRA_ARGUMENTS, getKotlinCompilerOptions())
        .customOptions(getKotlinFriendPaths(true))
        .build();
  }

  public String getSourceCompatibility() {
    return javaVersion(
        getProject()
            .getConvention()
            .getPlugin(JavaPluginConvention.class)
            .getSourceCompatibility());
  }

  public String getTargetCompatibility() {
    return javaVersion(
        getProject()
            .getConvention()
            .getPlugin(JavaPluginConvention.class)
            .getTargetCompatibility());
  }

  public String getMavenCoords() {
    String group = getProject().getGroup().toString();
    String id =
        getProject().getConvention().getPlugin(BasePluginConvention.class).getArchivesBaseName();
    String version = getProject().getVersion().toString();
    return String.join(":", group, id, version);
  }

  public boolean hasApplication() {
    return getProject().getPlugins().hasPlugin(ApplicationPlugin.class);
  }

  @Nullable
  public String getMainClass() {
    return getProject()
        .getConvention()
        .getPlugin(ApplicationPluginConvention.class)
        .getMainClassName();
  }

  public Set<String> getExcludes() {
    Jar jarTask = (Jar) getProject().getTasks().findByName(JavaPlugin.JAR_TASK_NAME);
    return jarTask != null ? jarTask.getExcludes() : ImmutableSet.of();
  }

  private Set<File> getMainSrcDirs() {
    return sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getAllJava().getSrcDirs();
  }

  private Set<File> getMainJavaResourceDirs() {
    return sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).getResources().getSrcDirs();
  }

  private Set<File> getTestSrcDirs() {
    return sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME).getAllJava().getSrcDirs();
  }

  private Set<File> getTestJavaResourceDirs() {
    return sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME).getResources().getSrcDirs();
  }

  private Set<File> getIntegrationTestSrcDirs() {
    // SourceSet might not be available to module
    try {
      return sourceSets.getByName(INTEGRATION_TEST_SOURCE_SET_NAME).getAllJava().getSrcDirs();
    } catch (UnknownDomainObjectException e) {
      return Collections.emptySet();
    }
  }

  private Set<File> getIntegrationTestJavaResourceDirs() {
    // SourceSet might not be available to module
    try {
      return sourceSets.getByName(INTEGRATION_TEST_SOURCE_SET_NAME).getResources().getSrcDirs();
    } catch (UnknownDomainObjectException e) {
      return Collections.emptySet();
    }
  }

  public static String javaVersion(JavaVersion version) {
    return version.getMajorVersion();
  }

  /**
   * For Kotlin tests, a special extra friend-paths argument needs to be passed to read internal
   * elements. See https://github.com/uber/okbuck/issues/709
   *
   * @param isTest
   * @return the list with all friend paths
   */
  public Map<String, List<String>> getKotlinFriendPaths(boolean isTest) {
    if (!isKotlin || !isTest) {
      return ImmutableMap.of();
    }

    return ImmutableMap.of("friend_paths", ImmutableList.of(":" + JvmBuckRuleComposer.src(this)));
  }

  protected List<String> getKotlinCompilerOptions() {
    if (!isKotlin) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<String> optionBuilder = ImmutableList.builder();
    optionBuilder.addAll(readKotlinCompilerArguments());
    if (getProject().getPlugins().hasPlugin(KotlinManager.KOTLIN_ALLOPEN_MODULE)) {
      AllOpenKotlinGradleSubplugin subplugin = getAllOpenKotlinGradleSubplugin();

      if (subplugin != null && fakeCompile != null) {
        List<SubpluginOption> options =
            subplugin.apply(getProject(), fakeCompile, fakeCompile, null, null, null);

        optionBuilder.add(
            "-Xplugin="
                + KotlinManager.KOTLIN_LIBRARIES_LOCATION
                + File.separator
                + KotlinManager.KOTLIN_ALLOPEN_JAR);

        for (SubpluginOption option : options) {
          optionBuilder.add("-P");
          optionBuilder.add(
              "plugin:org.jetbrains.kotlin.allopen:" + option.getKey() + "=" + option.getValue());
        }
      }
    }
    return optionBuilder.build();
  }

  private List<String> readKotlinCompilerArguments() {
    try {
      // Note: this is bad juju on Gradle 5.0, which would prefer you get the provider and lazily
      // eval.
      // We don't differentiate between test and non-test right now because Android projects don't
      // support test-only configuration. Non-android projects theoretically can, but let's wait for
      // someone to ask for that support first as it's not very common.
      Optional<KotlinCompile> kotlinCompileTask =
          getProject().getTasks().withType(KotlinCompile.class).stream().findFirst();
      if (!kotlinCompileTask.isPresent()) {
        return Collections.emptyList();
      }
      ImmutableMap.Builder<String, Optional<String>> optionBuilder = ImmutableMap.builder();
      KotlinJvmOptions options = kotlinCompileTask.get().getKotlinOptions();
      LinkedHashMap<String, Optional<String>> freeArgs = Maps.newLinkedHashMap();
      options.getFreeCompilerArgs().forEach(arg -> freeArgs.put(arg, Optional.empty()));
      optionBuilder.putAll(freeArgs);

      // Args from CommonToolArguments.kt and KotlinCommonToolOptions.kt
      if (options.getAllWarningsAsErrors()) {
        optionBuilder.put("-Werror", Optional.empty());
      }
      if (options.getSuppressWarnings()) {
        optionBuilder.put("-nowarn", Optional.empty());
      }
      if (options.getVerbose()) {
        optionBuilder.put("-verbose", Optional.empty());
      }

      // Args from K2JVMCompilerArguments.kt and KotlinJvmOptions.kt
      optionBuilder.put("-jvm-target", Optional.of(options.getJvmTarget()));
      if (options.getIncludeRuntime()) {
        optionBuilder.put("-include-runtime", Optional.empty());
      }
      String jdkHome = options.getJdkHome();
      if (jdkHome != null) {
        optionBuilder.put("-jdk-home", Optional.of(jdkHome));
      }
      if (options.getNoJdk()) {
        optionBuilder.put("-no-jdk", Optional.empty());
      }
      if (options.getNoStdlib()) {
        optionBuilder.put("-no-stdlib", Optional.empty());
      }
      if (options.getNoReflect()) {
        optionBuilder.put("-no-reflect", Optional.empty());
      }
      if (options.getJavaParameters()) {
        optionBuilder.put("-java-parameters", Optional.empty());
      }

      // In the future, could add any other compileKotlin configurations here

      // Return de-duping keys and sorting by them.
      return optionBuilder
          .build()
          .entrySet()
          .stream()
          .filter(distinctByKey(Map.Entry::getKey))
          .sorted(Comparator.comparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER))
          .flatMap(
              entry -> {
                if (entry.getValue().isPresent()) {
                  return ImmutableList.of(entry.getKey(), entry.getValue().get()).stream();
                } else {
                  return ImmutableList.of(entry.getKey()).stream();
                }
              })
          .collect(Collectors.toList());
    } catch (UnknownDomainObjectException ignored) {
      // Because why return null when you can throw an exception
      return Collections.emptyList();
    }
  }

  @Nullable
  private AllOpenKotlinGradleSubplugin getAllOpenKotlinGradleSubplugin() {
    for (KotlinGradleSubplugin subplugin :
        ServiceLoader.load(KotlinGradleSubplugin.class, getClass().getClassLoader())) {
      if (subplugin instanceof AllOpenKotlinGradleSubplugin) {
        return (AllOpenKotlinGradleSubplugin) subplugin;
      }
    }
    return null;
  }

  private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }

  /**
   * Get implementation target deps. deps = (runtimeClasspath(api + implementation + runtimeOnly)
   * intersection compileClasspath(api + implementation + compileOnly)) - api
   *
   * @return Target deps
   */
  public Set<Target> getTargetDeps(SourceSetType sourceSetType) {
    switch (sourceSetType) {
      case TEST:
        return Sets.intersection(getTest().getTargetDeps(), getTestProvided().getTargetDeps());

      case INTEGRATION_TEST:
        return Sets.intersection(getIntegrationTest().getTargetDeps(),
            getIntegrationTestProvided().getTargetDeps());

      default:
        return Sets.difference(
            Sets.intersection(getMain().getTargetDeps(), getProvided().getTargetDeps()),
            getApiTargetDeps());
    }
  }

  /**
   * Get api target deps. exportedDeps = api
   *
   * @return Target deps
   */
  public Set<Target> getTargetExportedDeps(SourceSetType sourceSetType) {
    switch (sourceSetType) {
      case TEST:
      case INTEGRATION_TEST:
        return ImmutableSet.of();

      default:
        return getApiTargetDeps();
    }
  }

  /**
   * Get apt target deps. exportedDeps = apt
   *
   * @return Target deps
   */
  public Set<Target> getTargetAptDeps(SourceSetType sourceSetType) {
    switch (sourceSetType) {
      case TEST:
        return getTestApt().getTargetDeps();
      case INTEGRATION_TEST:
        return getIntegrationTestApt().getTargetDeps();

      default:
        return getApt().getTargetDeps();
    }
  }

  /**
   * Get compileOnly target deps. compileOnlyDeps = compileClasspath(api + implementation +
   * compileOnly) - runtimeClasspath(api + implementation + runtimeOnly)
   *
   * @return CompileOnly Target deps
   */
  public Set<Target> getTargetProvidedDeps(SourceSetType sourceSetType) {
    switch (sourceSetType) {
      case TEST:
        return Sets.difference(getTestProvided().getTargetDeps(), getTest().getTargetDeps());
      case INTEGRATION_TEST:
        return Sets.difference(getIntegrationTestProvided().getTargetDeps(),
            getIntegrationTest().getTargetDeps());

      default:
        return Sets.difference(getProvided().getTargetDeps(), getMain().getTargetDeps());
    }
  }

  /**
   * Get implementation target deps. deps = (runtimeClasspath(api + implementation + runtimeOnly)
   * intersection compileClasspath(api + implementation + compileOnly)) - api
   *
   * @return Target deps
   */
  public Set<ExternalDependency> getExternalDeps(SourceSetType sourceSetType) {
    switch (sourceSetType) {
      case TEST:
        return Sets.intersection(getTest().getExternalDeps(), getTestProvided().getExternalDeps());
      case INTEGRATION_TEST:
        return Sets.intersection(getIntegrationTest().getExternalDeps(),
            getIntegrationTestProvided().getExternalDeps());

      default:
        return Sets.difference(
            Sets.intersection(getMain().getExternalDeps(), getProvided().getExternalDeps()),
            getApiExternalDeps());
    }
  }

  public Set<ExternalDependency> getExternalAarDeps(SourceSetType sourceSetType) {
    return filterAar(getExternalDeps(sourceSetType));
  }

  /**
   * Get api target deps. exportedDeps = api
   *
   * @return Target deps
   */
  public Set<ExternalDependency> getExternalExportedDeps(SourceSetType sourceSetType) {
    switch (sourceSetType) {
      case TEST:
      case INTEGRATION_TEST:
        return ImmutableSet.of();

      default:
        return getApiExternalDeps();
    }
  }

  public Set<ExternalDependency> getExternalExportedAarDeps(SourceSetType sourceSetType) {
    return filterAar(getExternalExportedDeps(sourceSetType));
  }

  /**
   * Get apt target deps. exportedDeps = apt
   *
   * @return Target deps
   */
  public Set<ExternalDependency> getExternalAptDeps(SourceSetType sourceSetType) {
    switch (sourceSetType) {
      case TEST:
        return filterJar(getTestApt().getExternalDeps());
      case INTEGRATION_TEST:
        return filterJar(getIntegrationTestApt().getExternalDeps());

      default:
        return filterJar(getApt().getExternalDeps());
    }
  }

  /**
   * Get compileOnly target deps. compileOnlyDeps = compileClasspath(api + implementation +
   * compileOnly) - runtimeClasspath(api + implementation + runtimeOnly)
   *
   * @return CompileOnly Target deps
   */
  public Set<ExternalDependency> getExternalProvidedDeps(SourceSetType sourceSetType) {
    switch (sourceSetType) {
      case TEST:
        return Sets.difference(getTestProvided().getExternalDeps(), getTest().getExternalDeps());
      case INTEGRATION_TEST:
        return Sets.difference(getIntegrationTestProvided().getExternalDeps(),
            getIntegrationTest().getExternalDeps());

      default:
        return Sets.difference(getProvided().getExternalDeps(), getMain().getExternalDeps());
    }
  }
}
