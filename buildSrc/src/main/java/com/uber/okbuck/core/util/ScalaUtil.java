package com.uber.okbuck.core.util;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.DependencyUtils;
import com.uber.okbuck.template.config.ScalaBuckFile;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

public final class ScalaUtil {

  private static final String SCALA_DEPS_CONFIG = "okbuck_scala_deps";

  public static final String SCALA_HOME_LOCATION =
      OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/scala_installation";

  private ScalaUtil() {}

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public static void setupScalaHome(Project rootProject, String scalaVersion) {
    Configuration scalaConfig = rootProject.getConfigurations().maybeCreate(SCALA_DEPS_CONFIG);
    rootProject
        .getDependencies()
        .add(SCALA_DEPS_CONFIG, "org.scala-lang:scala-compiler:" + scalaVersion);
    new DependencyCache(
            rootProject, DependencyUtils.createCacheDir(rootProject, SCALA_HOME_LOCATION))
        .build(scalaConfig);
    new ScalaBuckFile()
        .scalaVersion(scalaVersion)
        .render(rootProject.file(SCALA_HOME_LOCATION).toPath().resolve("BUCK"));
  }
}
