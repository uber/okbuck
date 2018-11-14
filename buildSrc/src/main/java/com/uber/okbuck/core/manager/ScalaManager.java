package com.uber.okbuck.core.manager;

import com.google.common.collect.Multimap;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.LoadStatementsUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.OkBuckExtension;
import com.uber.okbuck.template.core.Rule;
import com.uber.okbuck.template.jvm.JvmBinaryRule;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

public final class ScalaManager {

  private static final String SCALA_DEPS_CONFIG = "okbuck_scala_deps";

  public static final String SCALA_COMPILER_LOCATION =
      OkBuckGradlePlugin.WORKSPACE_PATH + "/scala_installation";

  private final Project rootProject;
  private final OkBuckExtension extension;
  @Nullable private Set<String> dependencies;

  public ScalaManager(Project rootProject, OkBuckExtension extension) {
    this.rootProject = rootProject;
    this.extension = extension;
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public Set<String> setupScalaHome(String scalaVersion) {
    Configuration scalaConfig = rootProject.getConfigurations().maybeCreate(SCALA_DEPS_CONFIG);
    rootProject
        .getDependencies()
        .add(SCALA_DEPS_CONFIG, "org.scala-lang:scala-compiler:" + scalaVersion);
    return dependencies =
        new DependencyCache(rootProject, ProjectUtil.getDependencyManager(rootProject))
            .build(scalaConfig);
  }

  public void finalizeDependencies() {
    if (dependencies != null) {
      List<Rule> scalaCompiler =
          Collections.singletonList(
              new JvmBinaryRule()
                  .mainClassName("scala.tools.nsc.Main")
                  .deps(BuckRuleComposer.external(dependencies))
                  .ruleType(RuleType.JAVA_BINARY.getBuckName())
                  .name("scala-compiler.jar")
                  .defaultVisibility());

      Multimap<String, String> loadStatements =
          LoadStatementsUtil.getLoadStatements(scalaCompiler, extension);

      File buckFile =
          rootProject
              .file(SCALA_COMPILER_LOCATION)
              .toPath()
              .resolve(OkBuckGradlePlugin.BUCK)
              .toFile();
      FileUtil.writeToBuckFile(loadStatements, scalaCompiler, buckFile);
    }
  }
}
