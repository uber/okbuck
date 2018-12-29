package com.uber.okbuck.core.model.jvm;

import com.android.builder.model.LintOptions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.annotation.AnnotationProcessorCache;
import com.uber.okbuck.core.manager.KotlinManager;
import com.uber.okbuck.core.manager.LintManager;
import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.core.model.base.Target;
import com.uber.okbuck.core.util.ProjectUtil;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
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
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin;
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption;

public class JvmTarget extends Target {

  public static final String MAIN = "main";

  private final String aptConfigurationName;
  private final String testAptConfigurationName;
  private final SourceSetContainer sourceSets;

  @Nullable private final AbstractCompile fakeCompile;

  public JvmTarget(Project project, String name) {
    this(
        project,
        name,
        JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME,
        JavaPlugin.TEST_ANNOTATION_PROCESSOR_CONFIGURATION_NAME);
  }

  public JvmTarget(
      Project project, String name, String aptConfigurationName, String testAptConfigurationName) {
    super(project, name);
    this.aptConfigurationName = aptConfigurationName;
    this.testAptConfigurationName = testAptConfigurationName;
    sourceSets = getProject().getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();

    Optional<Task> compileTask =
        project.getTasks().stream().filter(it -> it instanceof AbstractCompile).findFirst();
    if (compileTask.isPresent()) {
      fakeCompile = (AbstractCompile) compileTask.get();
    } else {
      fakeCompile = null;
    }
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

  /** Apt Scopes */
  public List<Scope> getAptScopes() {
    AnnotationProcessorCache apCache = ProjectUtil.getAnnotationProcessorCache(getProject());
    return apCache.getAnnotationProcessorScopes(getProject(), aptConfigurationName);
  }

  /** Test Apt Scopes */
  public List<Scope> getTestAptScopes() {
    AnnotationProcessorCache apCache = ProjectUtil.getAnnotationProcessorCache(getProject());
    return apCache.getAnnotationProcessorScopes(getProject(), testAptConfigurationName);
  }

  /** Apt Scope Used to get the annotation processor deps of the target. */
  public Scope getApt() {
    return getAptScopeForConfiguration(aptConfigurationName);
  }

  /** Test Apt Scope */
  public Scope getTestApt() {
    return getAptScopeForConfiguration(testAptConfigurationName);
  }

  protected Scope getAptScopeForConfiguration(String configurationName) {
    // If using annotation processor plugin, return an empty scope if there are no annotation
    // processors so no need to have any specified in the annotation processor deps list.
    if (!ProjectUtil.getAnnotationProcessorCache(getProject())
        .hasEmptyAnnotationProcessors(getProject(), configurationName)) {
      return Scope.builder(getProject()).build();
    }
    return Scope.builder(getProject()).configuration(configurationName).build();
  }

  protected Scope getAptScopeForConfiguration(Configuration configuration) {
    // If using annotation processor plugin, return an empty scope if there are no annotation
    // processors so no need to have any specified in the annotation processor deps list.
    if (!ProjectUtil.getAnnotationProcessorCache(getProject())
        .hasEmptyAnnotationProcessors(getProject(), configuration)) {
      return Scope.builder(getProject()).build();
    }
    return Scope.builder(getProject()).configuration(configuration).build();
  }

  /** Provided Scope */
  public Scope getProvided() {
    return Scope.builder(getProject())
        .configuration(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)
        .build();
  }

  /** Test Provided Scope */
  public Scope getTestProvided() {
    return Scope.builder(getProject())
        .configuration(JavaPlugin.TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME)
        .build();
  }

  /** Lint Scope */
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

  public boolean hasLintRegistry() {
    Jar jarTask = (Jar) getProject().getTasks().findByName(JavaPlugin.JAR_TASK_NAME);
    return jarTask != null
        && (jarTask.getManifest().getAttributes().containsKey("Lint-Registry")
            || jarTask.getManifest().getAttributes().containsKey("Lint-Registry-v2"));
  }

  /**
   * List of annotation processor classes. If annotation processor plugin is enabled returns the
   * annotation processor's UID.
   */
  public Set<String> getApPlugins() {
    return getAptScopes()
        .stream()
        .filter(scope -> !scope.getAnnotationProcessors().isEmpty())
        .map(Scope::getAnnotationProcessorsUID)
        .collect(Collectors.toSet());
  }

  /**
   * List of test annotation processor classes. If annotation processor plugin is enabled returns
   * the annotation processor's UID.
   */
  public Set<String> getTestApPlugins() {
    return getTestAptScopes()
        .stream()
        .filter(scope -> !scope.getAnnotationProcessors().isEmpty())
        .map(Scope::getAnnotationProcessorsUID)
        .collect(Collectors.toSet());
  }

  public Scope getMain() {
    JavaCompile compileJavaTask =
        (JavaCompile) getProject().getTasks().getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME);
    return Scope.builder(getProject())
        .configuration(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        .sourceDirs(getMainSrcDirs())
        .javaResourceDirs(getMainJavaResourceDirs())
        .compilerOptions(
            Scope.Builder.COMPILER.JAVA, compileJavaTask.getOptions().getCompilerArgs())
        .compilerOptions(Scope.Builder.COMPILER.KOTLIN, getKotlinCompilerOptions())
        .build();
  }

  public Scope getTest() {
    JavaCompile testCompileJavaTask =
        (JavaCompile) getProject().getTasks().getByName(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME);
    return Scope.builder(getProject())
        .configuration(JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        .sourceDirs(getTestSrcDirs())
        .javaResourceDirs(getTestJavaResourceDirs())
        .compilerOptions(
            Scope.Builder.COMPILER.JAVA, testCompileJavaTask.getOptions().getCompilerArgs())
        .compilerOptions(Scope.Builder.COMPILER.KOTLIN, getKotlinCompilerOptions())
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

  public static String javaVersion(JavaVersion version) {
    return version.getMajorVersion();
  }

  protected List<String> getKotlinCompilerOptions() {
    if (getProject().getPlugins().hasPlugin(KotlinManager.KOTLIN_ALLOPEN_MODULE)) {
      AllOpenKotlinGradleSubplugin subplugin = getAllOpenKotlinGradleSubplugin();

      if (subplugin == null || fakeCompile == null) {
        return ImmutableList.of();
      }

      List<SubpluginOption> options =
          subplugin.apply(getProject(), fakeCompile, fakeCompile, null, null, null);
      ImmutableList.Builder<String> optionBuilder = ImmutableList.builder();

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

      return optionBuilder.build();
    }
    return ImmutableList.of();
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
}
