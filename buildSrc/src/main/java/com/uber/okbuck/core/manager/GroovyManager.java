package com.uber.okbuck.core.manager;

import com.google.common.collect.ImmutableList;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.template.config.groovy.GroovyBuckFile;
import com.uber.okbuck.template.config.groovy.Groovyc;
import com.uber.okbuck.template.config.groovy.StartGroovy;
import com.uber.okbuck.template.core.Rule;
import groovy.lang.GroovySystem;
import java.io.File;
import java.util.Set;
import javax.annotation.Nullable;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

public final class GroovyManager {

  private static final String GROOVY_DEPS_CONFIG = "okbuck_groovy_deps";

  public static final String GROOVY_HOME_LOCATION =
      OkBuckGradlePlugin.WORKSPACE_PATH + "/groovy_installation";
  public static final String GROOVY_HOME = "groovy_home";
  public static final String GROOVY_HOME_TARGET = "//" + GROOVY_HOME_LOCATION + ":" + GROOVY_HOME;

  private static String groovyVersion = GroovySystem.getVersion();

  private final Project rootProject;
  private final BuckFileManager buckFileManager;
  @Nullable private Set<ExternalDependency> dependencies;

  public GroovyManager(Project rootProject, BuckFileManager buckFileManager) {
    this.rootProject = rootProject;
    this.buckFileManager = buckFileManager;
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void setupGroovyHome() {
    Configuration groovyConfig = rootProject.getConfigurations().maybeCreate(GROOVY_DEPS_CONFIG);
    rootProject
        .getDependencies()
        .add(GROOVY_DEPS_CONFIG, "org.codehaus.groovy:groovy:" + groovyVersion);
    dependencies =
        new DependencyCache(rootProject, ProjectUtil.getDependencyManager(rootProject))
            .build(groovyConfig);
  }

  public void finalizeDependencies() {
    File groovyHome = rootProject.file(GROOVY_HOME_LOCATION);
    FileUtil.deleteQuietly(groovyHome.toPath());

    if (dependencies != null && dependencies.size() > 0) {

      groovyHome.mkdirs();

      File groovyStarterConf = new File(groovyHome, "groovy-starter.conf");
      FileUtil.copyResourceToProject("groovy/conf/groovy-starter.conf", groovyStarterConf);

      File groovyc = new File(groovyHome, "groovyc");
      new Groovyc().groovyVersion(groovyVersion).render(groovyc);
      groovyc.setExecutable(true);

      File startGroovy = new File(groovyHome, "startGroovy");
      new StartGroovy().groovyVersion(groovyVersion).render(startGroovy);
      startGroovy.setExecutable(true);

      ExternalDependency groovyAll = dependencies.iterator().next();

      Rule groovyHomeRule =
          new GroovyBuckFile()
              .groovyAllJar(BuckRuleComposer.external(groovyAll))
              .groovyVersion(groovyVersion)
              .name(GROOVY_HOME);

      buckFileManager.writeToBuckFile(
          ImmutableList.of(groovyHomeRule),
          groovyHome.toPath().resolve(OkBuckGradlePlugin.BUCK).toFile());
    }
  }
}
