package com.uber.okbuck.core.task;

import static com.uber.okbuck.OkBuckGradlePlugin.OKBUCK_PREBUILT_FILE;
import static com.uber.okbuck.OkBuckGradlePlugin.OKBUCK_TARGETS_FILE;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.manager.BuckFileManager;
import com.uber.okbuck.core.manager.GroovyManager;
import com.uber.okbuck.core.manager.KotlinManager;
import com.uber.okbuck.core.manager.ScalaManager;
import com.uber.okbuck.core.model.base.ProjectType;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.core.util.ProguardUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.KotlinExtension;
import com.uber.okbuck.extension.OkBuckExtension;
import com.uber.okbuck.extension.RuleOverridesExtension;
import com.uber.okbuck.extension.ScalaExtension;
import com.uber.okbuck.generator.OkbuckBuckConfigGenerator;
import com.uber.okbuck.template.config.OkbuckPrebuilt;
import com.uber.okbuck.template.config.OkbuckTargets;
import com.uber.okbuck.template.core.Rule;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

@SuppressWarnings({"WeakerAccess", "unused", "ResultOfMethodCallIgnored", "NewApi"})
public class OkBuckTask extends DefaultTask {

  public static final String CLASSPATH_ABI_MACRO = "classpath_abi";

  @Nested public OkBuckExtension okBuckExtension;

  @Nested public KotlinExtension kotlinExtension;

  @Nested public ScalaExtension scalaExtension;

  private BuckFileManager buckFileManager;

  @Inject
  public OkBuckTask(
      OkBuckExtension okBuckExtension,
      KotlinExtension kotlinExtension,
      ScalaExtension scalaExtension,
      BuckFileManager buckFileManager) {
    this.okBuckExtension = okBuckExtension;
    this.kotlinExtension = kotlinExtension;
    this.scalaExtension = scalaExtension;

    this.buckFileManager = buckFileManager;

    // Never up to date; this task isn't safe to run incrementally.
    getOutputs().upToDateWhen(Specs.satisfyNone());
  }

  @TaskAction
  void okbuck() {
    // Fetch Groovy support deps if needed
    boolean hasGroovyLib =
        okBuckExtension
            .buckProjects
            .stream()
            .anyMatch(project -> ProjectUtil.getType(project) == ProjectType.GROOVY_LIB);
    if (hasGroovyLib) {
      ProjectUtil.getGroovyManager(getProject()).setupGroovyHome();
    }

    // Fetch Scala support deps if needed
    String scalaLibraryLocation;
    boolean hasScalaLib =
        okBuckExtension
            .buckProjects
            .stream()
            .anyMatch(project -> ProjectUtil.getType(project) == ProjectType.SCALA_LIB);
    if (hasScalaLib) {
      Set<ExternalDependency> scalaDeps =
          ProjectUtil.getScalaManager(getProject()).setupScalaHome(scalaExtension.version);
      scalaLibraryLocation =
          BuckRuleComposer.external(
              scalaDeps
                  .stream()
                  .filter(it -> it.getTargetName().contains("scala-library"))
                  .findFirst()
                  .get());
    } else {
      scalaLibraryLocation = "";
    }

    // Fetch Kotlin deps if needed
    if (kotlinExtension.version != null) {
      ProjectUtil.getKotlinManager(getProject()).setupKotlinHome(kotlinExtension.version);
    }

    generate(
        okBuckExtension,
        hasGroovyLib ? GroovyManager.GROOVY_HOME_TARGET : null,
        kotlinExtension.version != null ? KotlinManager.KOTLIN_HOME_TARGET : null,
        hasScalaLib ? ScalaManager.SCALA_COMPILER_LOCATION : null,
        hasScalaLib ? scalaLibraryLocation : null);
  }

  @Override
  public String getGroup() {
    return OkBuckGradlePlugin.GROUP;
  }

  @Override
  public String getDescription() {
    return "Okbuck task for the root project. Also sets up groovy and kotlin if required.";
  }

  @OutputFile
  public File okbuckTargets() {
    return getProject().file(OKBUCK_TARGETS_FILE);
  }

  @OutputFile
  public File okbuckPrebuilt() {
    return getProject().file(OKBUCK_PREBUILT_FILE);
  }

  @OutputFile
  public File dotBuckConfig() {
    return getProject().file(".buckconfig");
  }

  @OutputFile
  public File okbuckBuckConfig() {
    return getProject().file(OkBuckGradlePlugin.OKBUCK_CONFIG + "/okbuck.buckconfig");
  }

  @SuppressWarnings("NullAway")
  private void generate(
      OkBuckExtension okbuckExt,
      @Nullable String groovyHome,
      @Nullable String kotlinHome,
      @Nullable String scalaCompiler,
      @Nullable String scalaLibrary) {
    // generate empty .buckconfig if it does not exist
    try {
      dotBuckConfig().createNewFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Setup okbuck_targets.bzl
    new OkbuckTargets()
        .resourceExcludes(
            okBuckExtension
                .excludeResources
                .stream()
                .map(s -> "'" + s + "'")
                .collect(Collectors.toSet()))
        .classpathMacro(CLASSPATH_ABI_MACRO)
        .lintJvmArgs(okbuckExt.getLintExtension().jvmArgs)
        .enableLint(!okbuckExt.getLintExtension().disabled)
        .jetifierConfigurationTarget(
            BuckRuleComposer.fileRule(okbuckExt.getJetifierExtension().customConfigFile))
        .externalDependencyCache(okbuckExt.getExternalDependenciesExtension().getCache())
        .classpathExclusionRegex(okbuckExt.getLintExtension().classpathExclusionRegex)
        .useCompilationClasspath(okbuckExt.getLintExtension().useCompilationClasspath)
        .render(okbuckTargets());

    // Setup okbuck_prebuilt.bzl
    Map<String, RuleOverridesExtension.OverrideSetting> overrides =
        okbuckExt.getRuleOverridesExtension().getOverrides();
    Multimap<String, String> loadStatements = TreeMultimap.create();

    RuleOverridesExtension.OverrideSetting aarSetting =
        overrides.get(RuleType.ANDROID_PREBUILT_AAR.getBuckName());
    RuleOverridesExtension.OverrideSetting jarSetting =
        overrides.get(RuleType.PREBUILT_JAR.getBuckName());

    loadStatements.put(aarSetting.getImportLocation(), aarSetting.getNewRuleName());
    loadStatements.put(jarSetting.getImportLocation(), jarSetting.getNewRuleName());

    Rule okbuckPrebuiltRule =
        new OkbuckPrebuilt()
            .prebuiltAarRule(aarSetting.getNewRuleName())
            .prebuiltJarRule(jarSetting.getNewRuleName());

    buckFileManager.writeToBuckFile(
        ImmutableList.of(okbuckPrebuiltRule), okbuckPrebuilt(), loadStatements);

    // generate .buckconfig.okbuck
    OkbuckBuckConfigGenerator.generate(
            okbuckExt,
            groovyHome,
            kotlinHome,
            scalaCompiler,
            scalaLibrary,
            ProguardUtil.getProguardJarPath(getProject()),
            repositoryMap())
        .render(okbuckBuckConfig());
  }

  private LinkedHashMap<String, String> repositoryMap() {
    LinkedHashMap<String, String> repositories = new LinkedHashMap<>();
    addRepositories(getProject().getRootProject(), repositories);
    getProject()
        .getRootProject()
        .getSubprojects()
        .forEach(
            subProject -> {
              addRepositories(subProject, repositories);
            });

    return repositories;
  }

  private static void addRepositories(Project project, LinkedHashMap<String, String> repositories) {
    project
        .getRepositories()
        .forEach(
            repository -> {
              if (repository instanceof MavenArtifactRepository) {
                MavenArtifactRepository mavenRepository = (MavenArtifactRepository) repository;
                String name = mavenRepository.getName().toLowerCase();
                String url = mavenRepository.getUrl().toString();
                if (!repositories.containsKey(name) && !url.startsWith("file")) {
                  repositories.put(name, url);
                }
              }
            });
  }
}
