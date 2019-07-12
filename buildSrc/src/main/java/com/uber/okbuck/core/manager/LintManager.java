package com.uber.okbuck.core.manager;

import com.android.projectmodel.JavaLibrary;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.template.core.Rule;
import com.uber.okbuck.template.java.NativePrebuilt;
import com.uber.okbuck.template.jvm.JvmBinaryRule;
import java.io.File;
import java.nio.file.Path;
import java.util.Set;
import javax.annotation.Nullable;
import org.gradle.api.Project;

public final class LintManager {

  private static final String LINT_DEPS_CACHE = OkBuckGradlePlugin.WORKSPACE_PATH + "/lint";
  private static final String LINT_BINARY_RULE_NAME = "okbuck_lint";
  private static final String LINT_DUMMY_JAR = "lint-dummy.jar";

  private static final String LINT_GROUP = "com.android.tools.lint";
  private static final String LINT_MODULE = "lint";
  private static final String LINT_DEPS_CONFIG = OkBuckGradlePlugin.BUCK_LINT + "_deps";
  private static final ImmutableSet<String> LINT_BINARY_EXCLUDES =
      ImmutableSet.of("META-INF/.*\\.SF", "META-INF/.*\\.DSA", "META-INF/.*\\.RSA");
  private static final String LINT_CLI_CLASS = "com.uber.okbuck.android.lint.AndroidLintCli";

  private static final String ANDROID_LINT_CLI_JAR = "android-lint-cli.jar";
  private static final String ANDROID_LINT_CLI_RULE_NAME = "android-lint-cli";

  private final Project project;
  private final String lintBuckFile;
  private final BuckFileManager buckFileManager;

  private Set<ExternalDependency> dependencies;
  private DependencyCache lintDepCache;

  @SuppressWarnings("NullAway")
  public LintManager(Project project, String lintBuckFile, BuckFileManager buckFileManager) {
    this.project = project;
    this.lintBuckFile = lintBuckFile;
    this.buckFileManager = buckFileManager;
  }

  @Nullable
  public static String getDefaultLintVersion(Project buckProject) {
    return ProjectUtil.findVersionInClasspath(buckProject, LINT_GROUP, LINT_MODULE);
  }

  public void fetchLintDeps(String version) {
    project.getConfigurations().maybeCreate(LINT_DEPS_CONFIG);
    project.getDependencies().add(LINT_DEPS_CONFIG, LINT_GROUP + ":" + LINT_MODULE + ":" + version);

    getLintDepsCache();
  }

  public DependencyCache getLintDepsCache() {
    if (lintDepCache == null) {
      lintDepCache = new DependencyCache(project, ProjectUtil.getDependencyManager(project));

      dependencies =
          lintDepCache.build(
              project.getRootProject().getConfigurations().getByName(LINT_DEPS_CONFIG));
    }
    return lintDepCache;
  }

  public void finalizeDependencies() {
    Path lintCache = project.file(LINT_DEPS_CACHE).toPath();
    FileUtil.deleteQuietly(lintCache);

    if (dependencies != null && dependencies.size() > 0) {
      lintCache.toFile().mkdirs();

      new JvmBinaryRule()
          .mainClassName("")
          .excludes(LINT_BINARY_EXCLUDES)
          .defaultVisibility()
          .name(LINT_BINARY_RULE_NAME)
          .ruleType(RuleType.JAVA_BINARY.getBuckName());

      ImmutableList.Builder<Rule> rulesBuilder = new ImmutableList.Builder<>();

      Set<String> stringDependencies = BuckRuleComposer.external(dependencies);
      stringDependencies.add(":" + ANDROID_LINT_CLI_RULE_NAME);

      rulesBuilder.add(
          new JvmBinaryRule()
              .excludes(LINT_BINARY_EXCLUDES)
              .mainClassName(LINT_CLI_CLASS)
              .deps(stringDependencies)
              .ruleType(RuleType.JAVA_BINARY.getBuckName())
              .name(LINT_BINARY_RULE_NAME)
              .defaultVisibility());

      rulesBuilder.add(
          new NativePrebuilt()
              .prebuiltType(RuleType.PREBUILT_JAR.getProperties().get(0))
              .prebuilt(ANDROID_LINT_CLI_JAR)
              .ruleType(RuleType.PREBUILT_JAR.getBuckName())
              .name(ANDROID_LINT_CLI_RULE_NAME));

      rulesBuilder.add(
          new NativePrebuilt()
              .prebuiltType(RuleType.PREBUILT_JAR.getProperties().get(0))
              .prebuilt(LINT_DUMMY_JAR)
              .ruleType(RuleType.PREBUILT_JAR.getBuckName())
              .name(LINT_DUMMY_JAR));

      FileUtil.copyResourceToProject(
          "lint/" + ANDROID_LINT_CLI_JAR, new File(LINT_DEPS_CACHE, ANDROID_LINT_CLI_JAR));

      FileUtil.copyResourceToProject(
          "lint/" + LINT_DUMMY_JAR, new File(LINT_DEPS_CACHE, LINT_DUMMY_JAR));

      buckFileManager.writeToBuckFile(
          rulesBuilder.build(), project.getRootProject().file(lintBuckFile));
    }
  }
}
