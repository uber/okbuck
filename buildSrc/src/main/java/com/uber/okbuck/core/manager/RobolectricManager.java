package com.uber.okbuck.core.manager;

import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

public final class RobolectricManager {

  private static final String ROBOLECTRIC_RUNTIME = "robolectricRuntime";
  public static final String ROBOLECTRIC_CACHE =
      OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/robolectric";

  private final Project rootProject;
  private ImmutableSet<String> dependencies;

  public RobolectricManager(Project rootProject) {
    this.rootProject = rootProject;
  }

  public void download() {
    ImmutableSet.Builder<Configuration> runtimeDeps = ImmutableSet.builder();

    for (API api : EnumSet.allOf(API.class)) {
      Configuration runtimeApi =
          rootProject.getConfigurations().maybeCreate(ROBOLECTRIC_RUNTIME + "_" + api.name());
      rootProject.getDependencies().add(runtimeApi.getName(), api.getCoordinates());
      runtimeDeps.add(runtimeApi);
    }

    DependencyCache dependencyCache =
        new DependencyCache(rootProject, ProjectUtil.getDependencyManager(rootProject));

    dependencies =
        runtimeDeps
            .build()
            .stream()
            .map(dependencyCache::build)
            .flatMap(Set::stream)
            .collect(com.uber.okbuck.core.util.MoreCollectors.toImmutableSet());
  }

  public void finalizeDependencies() {
    if (dependencies != null) {
      Path robolectricCache = rootProject.file(ROBOLECTRIC_CACHE).toPath();
      FileUtil.deleteQuietly(robolectricCache);
      robolectricCache.toFile().mkdirs();

      dependencies.forEach(
          dependency -> {
            Path fromPath = rootProject.file(dependency + ".jar").toPath();
            Path toPath =
                robolectricCache.resolve(fromPath.getFileName().toString().replace("--", "-"));

            try {
              Files.createLink(toPath, fromPath.toRealPath());
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
    }
  }

  @SuppressWarnings("unused")
  enum API {
    API_16("4.1.2_r1", "r1"),
    API_17("4.2.2_r1.2", "r1"),
    API_18("4.3_r2", "r1"),
    API_19("4.4_r1", "r2"),
    API_21("5.0.2_r3", "r0"),
    API_22("5.1.1_r9", "r2"),
    API_23("6.0.1_r3", "r1"),
    API_24("7.0.0_r1", "r1"),
    API_25("7.1.0_r7", "r1"),
    API_26("8.0.0_r4", "r1"),
    API_27("8.1.0", "4611349"),
    API_28("P", "4651975");

    private final String androidVersion;
    private final String frameworkSdkBuildVersion;

    API(String androidVersion, String frameworkSdkBuildVersion) {
      this.androidVersion = androidVersion;
      this.frameworkSdkBuildVersion = frameworkSdkBuildVersion;
    }

    String getCoordinates() {
      return "org.robolectric:android-all:"
          + androidVersion
          + "-robolectric-"
          + frameworkSdkBuildVersion;
    }
  }
}
