package com.uber.okbuck.core.manager;

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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;

public final class LintManager {

  private static final String LINT_DEPS_CACHE = OkBuckGradlePlugin.WORKSPACE_PATH + "/lint";
  private static final String LINT_BINARY_RULE_NAME = "okbuck_lint";
  private static final String LINT_DUMMY_JAR = "lint-dummy.jar";

  private static final String LINT_GROUP = "com.android.tools.lint";
  private static final String LINT_MODULE = "lint";
  private static final String LINT_DEPS_CONFIG = OkBuckGradlePlugin.BUCK_LINT + "_deps";
  private static final String LINT_VERSION_FILE = LINT_DEPS_CACHE + "/.lintVersion";
  private static final ImmutableSet<String> LINT_BINARY_EXCLUDES =
      ImmutableSet.of("META-INF/.*\\.SF", "META-INF/.*\\.DSA", "META-INF/.*\\.RSA");
  private static final String LINT_CLI_CLASS = "com.android.tools.lint.Main";

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
    // Invalidate lint deps when versions change
    File lintVersionFile = project.file(LINT_VERSION_FILE);
    try {
      if (!lintVersionFile.exists()
          || !version.equals(
              new String(Files.readAllBytes(lintVersionFile.toPath()), StandardCharsets.UTF_8))) {
        FileUtils.deleteDirectory(lintVersionFile.getParentFile());
        lintVersionFile.getParentFile().mkdirs();
        Files.write(lintVersionFile.toPath(), Collections.singleton(version));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

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
    if (dependencies != null && dependencies.size() > 0) {
      new JvmBinaryRule()
          .mainClassName("")
          .excludes(LINT_BINARY_EXCLUDES)
          .defaultVisibility()
          .name(LINT_BINARY_RULE_NAME)
          .ruleType(RuleType.JAVA_BINARY.getBuckName());

      ImmutableList.Builder<Rule> rulesBuilder = new ImmutableList.Builder<>();
      rulesBuilder.add(
          new JvmBinaryRule()
              .excludes(LINT_BINARY_EXCLUDES)
              .mainClassName(LINT_CLI_CLASS)
              .deps(BuckRuleComposer.external(dependencies))
              .ruleType(RuleType.JAVA_BINARY.getBuckName())
              .name(LINT_BINARY_RULE_NAME)
              .defaultVisibility());

      rulesBuilder.add(
          new NativePrebuilt()
              .prebuiltType(RuleType.PREBUILT_JAR.getProperties().get(0))
              .prebuilt(LINT_DUMMY_JAR)
              .ruleType(RuleType.PREBUILT_JAR.getBuckName())
              .name(LINT_DUMMY_JAR));

      FileUtil.copyResourceToProject(
          "lint/" + LINT_DUMMY_JAR, new File(LINT_DEPS_CACHE, LINT_DUMMY_JAR));

      buckFileManager.writeToBuckFile(
          rulesBuilder.build(), project.getRootProject().file(lintBuckFile));
    }
  }
}
