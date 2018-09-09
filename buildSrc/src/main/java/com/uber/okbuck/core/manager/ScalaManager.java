package com.uber.okbuck.core.manager;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.template.config.ScalaBuckFile;
import java.util.Set;
import javax.annotation.Nullable;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

public final class ScalaManager {

  private static final String SCALA_DEPS_CONFIG = "okbuck_scala_deps";

  public static final String SCALA_COMPILER_LOCATION =
      OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/scala_installation";

  private final Project rootProject;
  @Nullable private Set<String> dependencies;

  public ScalaManager(Project rootProject) {
    this.rootProject = rootProject;
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
      new ScalaBuckFile()
          .deps(BuckRuleComposer.external(dependencies))
          .render(rootProject.file(SCALA_COMPILER_LOCATION).toPath().resolve("BUCK"));
    }
  }
}
