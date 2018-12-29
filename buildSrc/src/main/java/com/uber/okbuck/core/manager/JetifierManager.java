package com.uber.okbuck.core.manager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.sun.istack.Nullable;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.JetifierExtension;
import com.uber.okbuck.extension.OkBuckExtension;
import com.uber.okbuck.template.core.Rule;
import com.uber.okbuck.template.java.NativePrebuilt;
import com.uber.okbuck.template.jvm.JvmBinaryRule;
import java.io.File;
import java.nio.file.Paths;
import java.util.Set;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JetifierManager {

  private static final Logger LOG = LoggerFactory.getLogger(JetifierManager.class);

  private static final String JETIFIER_LOCATION = OkBuckGradlePlugin.WORKSPACE_PATH + "/jetifier";
  private static final String JETIFIER_CONFIG_FILE = "custom_mapping.config";
  private static final String JETIFIER_CONFIG_LOCATION =
      JETIFIER_LOCATION + "/" + JETIFIER_CONFIG_FILE;
  private static final String JETIFIER_BUCK_FILE = JETIFIER_LOCATION + "/BUCK";
  private static final String JETIFIER_DEPS_CONFIG = "okbuck_jetifier_deps";
  private static final String JETIFIER_GROUP = "com.android.tools.build.jetifier";
  private static final String JETIFIER_CLI_CLASS =
      "com.android.tools.build.jetifier.standalone.Main";
  private static final String JETIFIER_BINARY_RULE_NAME = "okbuck_jetifier";
  private static final String COMMONS_CLI_DEP = "commons-cli:commons-cli:1.3.1";

  private static final ImmutableList<String> JETIFIER_MODULES =
      ImmutableList.of("jetifier-core", "jetifier-processor");
  private static final ImmutableList<String> INTERNAL_MODULES =
      ImmutableList.of("jetifier-standalone.jar");

  @Nullable private Set<ExternalDependency> dependencies;
  private final Project project;
  private final BuckFileManager buckFileManager;
  private final OkBuckExtension okBuckExtension;

  public JetifierManager(
      Project project, BuckFileManager buckFileManager, OkBuckExtension okBuckExtension) {
    this.project = project;
    this.buckFileManager = buckFileManager;
    this.okBuckExtension = okBuckExtension;
  }

  public static boolean isJetifierEnabled(Project project) {
    Object prop = project.findProperty("android.enableJetifier");
    return prop != null ? Boolean.valueOf((String) prop) : false;
  }

  public void setupJetifier(String version) {
    if (!version.equals(JetifierExtension.DEFAULT_JETIFIER_VERSION)) {
      LOG.warn(
          "Using jetifier version other than %s; This might result in problems with the tool",
          JetifierExtension.DEFAULT_JETIFIER_VERSION);
    }

    Configuration jetifierConfig = project.getConfigurations().maybeCreate(JETIFIER_DEPS_CONFIG);
    DependencyHandler handler = project.getDependencies();
    JETIFIER_MODULES
        .stream()
        .map(module -> String.format("%s:%s:%s", JETIFIER_GROUP, module, version))
        .forEach(dependency -> handler.add(JETIFIER_DEPS_CONFIG, dependency));
    handler.add(JETIFIER_DEPS_CONFIG, COMMONS_CLI_DEP);

    dependencies =
        new DependencyCache(project, ProjectUtil.getDependencyManager(project))
            .build(jetifierConfig);
  }

  public void finalizeDependencies() {
    if (dependencies != null && dependencies.size() > 0) {
      ImmutableList.Builder<Rule> rulesBuilder = new ImmutableList.Builder<>();
      ImmutableSet.Builder<String> binaryDependencies = ImmutableSet.builder();
      binaryDependencies.addAll(BuckRuleComposer.external(dependencies));

      new File(JETIFIER_LOCATION).mkdirs();
      for (String module : INTERNAL_MODULES) {
        FileUtil.copyResourceToProject("jetifier/" + module, new File(JETIFIER_LOCATION, module));
        rulesBuilder.add(
            new NativePrebuilt()
                .prebuiltType(RuleType.PREBUILT_JAR.getProperties().get(0))
                .prebuilt(module)
                .ruleType(RuleType.PREBUILT_JAR.getBuckName())
                .name(module));
        binaryDependencies.add(":" + module);
      }

      rulesBuilder.add(
          new JvmBinaryRule()
              .mainClassName(JETIFIER_CLI_CLASS)
              .deps(binaryDependencies.build())
              .ruleType(RuleType.JAVA_BINARY.getBuckName())
              .name(JETIFIER_BINARY_RULE_NAME)
              .defaultVisibility());

      if (okBuckExtension.getJetifierExtension().customConfigFile != null) {
        File configFile =
            project.file(Paths.get(okBuckExtension.getJetifierExtension().customConfigFile));

        String relativeConfigPath =
            FileUtil.getRelativePath(project.getRootProject().getProjectDir(), configFile);
        ProjectUtil.getPlugin(project.getRootProject()).exportedPaths.add(relativeConfigPath);
      }

      buckFileManager.writeToBuckFile(
          rulesBuilder.build(), project.getRootProject().file(JETIFIER_BUCK_FILE));
    }
  }
}
