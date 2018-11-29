package com.uber.okbuck.core.manager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.template.config.SymlinkBuckFile;
import com.uber.okbuck.template.core.Rule;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

public final class RobolectricManager {

  private static final String ROBOLECTRIC_RUNTIME = "robolectricRuntime";
  private static final String ROBOLECTRIC_CACHE =
      OkBuckGradlePlugin.WORKSPACE_PATH + "/robolectric";
  private static final String ROBOLECTRIC_TARGET_NAME = "robolectric_cache";
  public static final String ROBOLECTRIC_CACHE_TARGET =
      "//" + ROBOLECTRIC_CACHE + ":" + ROBOLECTRIC_TARGET_NAME;

  private final Project rootProject;
  private final BuckFileManager buckFileManager;
  @Nullable private ImmutableSet<ExternalDependency> dependencies;

  public RobolectricManager(Project rootProject, BuckFileManager buckFileManager) {
    this.rootProject = rootProject;
    this.buckFileManager = buckFileManager;
  }

  public void download() {
    ImmutableSet.Builder<Configuration> runtimeDeps = ImmutableSet.builder();

    Set<API> apisToDownload;
    Set<String> configuredApis =
        ProjectUtil.getOkBuckExtension(rootProject).getTestExtension().robolectricApis;
    if (configuredApis != null) {
      apisToDownload = configuredApis.stream().map(API::from).collect(Collectors.toSet());
    } else {
      apisToDownload = EnumSet.allOf(API.class);
    }

    for (API api : apisToDownload) {
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
    if (dependencies != null && dependencies.size() > 0) {
      Path robolectricCache = rootProject.file(ROBOLECTRIC_CACHE).toPath();
      FileUtil.deleteQuietly(robolectricCache);
      robolectricCache.toFile().mkdirs();

      Map<String, String> targetsNameMap =
          dependencies
              .stream()
              .collect(
                  Collectors.toMap(BuckRuleComposer::external, ExternalDependency::getTargetName));

      Rule fileGroup =
          new SymlinkBuckFile()
              .targetsNameMap(targetsNameMap)
              .base("")
              .name(ROBOLECTRIC_TARGET_NAME);

      buckFileManager.writeToBuckFile(
          ImmutableList.of(fileGroup), robolectricCache.resolve(OkBuckGradlePlugin.BUCK).toFile());
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
    API_P("P", "4651975"),
    API_28("9", "4799589");

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

    static API from(String apiLevel) {
      switch (apiLevel) {
        case "16":
          return API_16;
        case "17":
          return API_17;
        case "18":
          return API_18;
        case "19":
          return API_19;
        case "21":
          return API_21;
        case "22":
          return API_22;
        case "23":
          return API_23;
        case "24":
          return API_24;
        case "25":
          return API_25;
        case "26":
          return API_26;
        case "27":
          return API_27;
        case "28":
          return API_28;
        case "P":
          return API_P;
        default:
          throw new IllegalStateException("Unknown Robolectric API Level: " + apiLevel);
      }
    }
  }
}
