package com.uber.okbuck.core.util;

import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.DependencyUtils;
import java.util.EnumSet;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

public final class RobolectricUtil {

  private static final String ROBOLECTRIC_RUNTIME = "robolectricRuntime";
  public static final String ROBOLECTRIC_CACHE =
      OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/robolectric";

  private RobolectricUtil() {}

  public static void download(Project project) {
    ImmutableSet.Builder<Configuration> runtimeDeps = ImmutableSet.builder();

    for (API api : EnumSet.allOf(API.class)) {
      Configuration runtimeApi =
          project.getConfigurations().maybeCreate(ROBOLECTRIC_RUNTIME + "_" + api.name());
      project.getDependencies().add(runtimeApi.getName(), api.getCoordinates());
      runtimeDeps.add(runtimeApi);
    }

    DependencyCache dependencyCache =
        new DependencyCache(project, DependencyUtils.createCacheDir(project, ROBOLECTRIC_CACHE));
    for (Configuration configuration : runtimeDeps.build()) {
      dependencyCache.build(configuration, false);
    }
    dependencyCache.cleanup();
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
