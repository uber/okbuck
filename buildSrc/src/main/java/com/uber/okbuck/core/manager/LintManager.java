package com.uber.okbuck.core.manager;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.java.LintBinaryComposer;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.template.core.Rule;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.jetbrains.annotations.Nullable;

public final class LintManager {

  public static final String LINT_DEPS_CACHE = OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/lint";
  public static final String LINT_DEPS_RULE = "//" + LINT_DEPS_CACHE + ":okbuck_lint";

  private static final String LINT_GROUP = "com.android.tools.lint";
  private static final String LINT_MODULE = "lint";
  private static final String LINT_DEPS_CONFIG = OkBuckGradlePlugin.BUCK_LINT + "_deps";
  private static final String LINT_VERSION_FILE = LINT_DEPS_CACHE + "/.lintVersion";

  private final Map<String, String> lintConfig = new ConcurrentHashMap<>();

  private final Project project;
  private final String lintBuckFile;

  private Set<String> dependencies;

  public LintManager(Project project, String lintBuckFile) {
    this.project = project;
    this.lintBuckFile = lintBuckFile;
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
          || !version.equals(new String(Files.readAllBytes(lintVersionFile.toPath())))) {
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
    OkBuckGradlePlugin okBuckGradlePlugin = ProjectUtil.getPlugin(project);
    if (okBuckGradlePlugin.lintDepCache == null) {
      okBuckGradlePlugin.lintDepCache =
          new DependencyCache(project, ProjectUtil.getDependencyManager(project));

      dependencies =
          okBuckGradlePlugin.lintDepCache.build(
              project.getRootProject().getConfigurations().getByName(LINT_DEPS_CONFIG));
    }
    return okBuckGradlePlugin.lintDepCache;
  }

  public String lintConfig(File config) {
    String configName =
        FileUtil.getRelativePath(project.getProjectDir(), config).replaceAll("/", "_");
    return lintConfig.computeIfAbsent(
        configName,
        key -> {
          File configFile = project.file(LINT_DEPS_CACHE + "/" + configName);
          try {
            FileUtils.copyFile(config, configFile);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          return "//" + LINT_DEPS_CACHE + ":" + configName;
        });
  }

  public void finalizeDependencies() {
    if (dependencies != null) {
      List<Rule> rules = LintBinaryComposer.compose(dependencies, lintConfig.keySet());
      File buckFile = project.getRootProject().file(lintBuckFile);
      FileUtil.writeToBuckFile(rules, buckFile);
    }
  }
}
