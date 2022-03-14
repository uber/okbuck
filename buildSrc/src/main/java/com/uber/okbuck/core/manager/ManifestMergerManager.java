package com.uber.okbuck.core.manager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.OExternalDependency;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.OkBuckExtension;
import com.uber.okbuck.template.core.Rule;
import com.uber.okbuck.template.java.NativePrebuilt;
import com.uber.okbuck.template.jvm.JvmBinaryRule;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;

public final class ManifestMergerManager {

  private static final String MANIFEST_MERGER_GROUP = "com.android.tools.build";
  private static final String MANIFEST_MERGER_MODULE = "manifest-merger";
  private static final String MANIFEST_MERGER_CACHE =
      OkBuckGradlePlugin.WORKSPACE_PATH + "/manifest-merger";
  private static final ImmutableSet<String> MANIFEST_MERGER_EXCLUDES =
      ImmutableSet.of("META-INF/.*\\\\.SF", "META-INF/.*\\\\.DSA", "META-INF/.*\\\\.RSA");
  private static final String MANIFEST_MERGER_CLI_CLASS =
      "com.uber.okbuck.manifmerger.ManifestMergerCli";

  private static final String MANIFEST_MERGER_RULE_NAME = "okbuck_manifest_merger";
  private static final String MANIFEST_MERGER_CLI_JAR = "manifest-merger-cli.jar";
  private static final String MANIFEST_MERGER_CLI_RULE_NAME = "manifest-merger-cli";

  private final Project rootProject;
  private final BuckFileManager buckFileManager;

  @Nullable private ImmutableSet<OExternalDependency> dependencies;

  public ManifestMergerManager(Project rootProject, BuckFileManager buckFileManager) {
    this.rootProject = rootProject;
    this.buckFileManager = buckFileManager;
  }

  public void fetchManifestMergerDeps() {
    boolean hasAndroidProject =
        rootProject.getAllprojects().stream().anyMatch(ProjectUtil::isAndroidType);

    if (hasAndroidProject) {
      Configuration manifestMergerConfiguration =
          rootProject
              .getConfigurations()
              .detachedConfiguration(
                  new DefaultExternalModuleDependency(
                      MANIFEST_MERGER_GROUP,
                      MANIFEST_MERGER_MODULE,
                      ProjectUtil.findVersionInClasspath(
                          rootProject, MANIFEST_MERGER_GROUP, MANIFEST_MERGER_MODULE)));

      dependencies =
          ImmutableSet.copyOf(
              new DependencyCache(rootProject, ProjectUtil.getDependencyManager(rootProject))
                  .build(manifestMergerConfiguration));
    }
  }

  public void finalizeDependencies(OkBuckExtension okBuckExtension) {
    Path manfistMergerCache = rootProject.file(MANIFEST_MERGER_CACHE).toPath();
    FileUtil.deleteQuietly(manfistMergerCache);

    if (dependencies != null && dependencies.size() > 0) {
      manfistMergerCache.toFile().mkdirs();

      FileUtil.copyResourceToProject(
          "manifest/" + MANIFEST_MERGER_CLI_JAR,
          new File(manfistMergerCache.toFile(), MANIFEST_MERGER_CLI_JAR));

      Set<String> deps = BuckRuleComposer.external(dependencies);
      deps.add(":" + MANIFEST_MERGER_CLI_RULE_NAME);

      List<Rule> rules =
          ImmutableList.of(
              new JvmBinaryRule()
                  .excludes(MANIFEST_MERGER_EXCLUDES)
                  .mainClassName(MANIFEST_MERGER_CLI_CLASS)
                  .deps(deps)
                  .ruleType(RuleType.JAVA_BINARY.getBuckName())
                  .name(MANIFEST_MERGER_RULE_NAME)
                  .defaultVisibility(),
              new NativePrebuilt()
                  .prebuiltType(RuleType.PREBUILT_JAR.getProperties().get(0))
                  .prebuilt(MANIFEST_MERGER_CLI_JAR)
                  .ruleType(RuleType.PREBUILT_JAR.getBuckName())
                  .name(MANIFEST_MERGER_CLI_RULE_NAME));

      buckFileManager.writeToBuckFile(rules,
          new File(manfistMergerCache.toFile(), okBuckExtension.buildFileName));
    }
  }
}
