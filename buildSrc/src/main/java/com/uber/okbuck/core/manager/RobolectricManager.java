package com.uber.okbuck.core.manager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.OExternalDependency;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.MoreCollectors;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.OkBuckExtension;
import com.uber.okbuck.template.config.SymlinkBuckFile;
import com.uber.okbuck.template.core.Rule;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
  @Nullable private ImmutableSet<OExternalDependency> dependencies;

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

    String preinstrumentedVersion =
        ProjectUtil.getOkBuckExtension(rootProject).getTestExtension().robolectricPreinstrumentedVersion;

    for (API api : apisToDownload) {
      Configuration runtimeApi =
          rootProject.getConfigurations().maybeCreate(ROBOLECTRIC_RUNTIME + "_" + api.name());
      rootProject.getDependencies().add(runtimeApi.getName(), api.getCoordinates(preinstrumentedVersion));
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
            .collect(MoreCollectors.toImmutableSet());
  }

  public void finalizeDependencies(OkBuckExtension okBuckExtension) {
    Path robolectricCache = rootProject.file(ROBOLECTRIC_CACHE).toPath();
    FileUtil.deleteQuietly(robolectricCache);

    if (dependencies != null && dependencies.size() > 0) {
      robolectricCache.toFile().mkdirs();

      Map<String, String> targetsNameMap =
          dependencies
              .stream()
              .collect(
                  Collectors.toMap(
                      BuckRuleComposer::external,
                      OExternalDependency::getTargetName,
                      (v1, v2) -> {
                        throw new IllegalStateException(
                            String.format("Duplicate key for values %s and %s", v1, v2));
                      },
                      TreeMap::new));

      Rule fileGroup =
          new SymlinkBuckFile()
              .targetsNameMap(targetsNameMap)
              .base("")
              .name(ROBOLECTRIC_TARGET_NAME);

      buckFileManager.writeToBuckFile(
          ImmutableList.of(fileGroup),
          robolectricCache.resolve(okBuckExtension.buildFileName).toFile());
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
    API_28("9", "4913185-2"),
    API_29("10", "5803371"),
    API_30("11", "6757853"),
    API_31("12", "7732740"),
    API_32("12.1", "8229987"),
    API_33("13", "9030017");

    private final String androidVersion;
    private final String frameworkSdkBuildVersion;

    API(String androidVersion, String frameworkSdkBuildVersion) {
      this.androidVersion = androidVersion;
      this.frameworkSdkBuildVersion = frameworkSdkBuildVersion;
    }

    String getCoordinates(String preinstrumentedVersion) {
      return "org.robolectric:android-all-instrumented:"
          + androidVersion
          + "-robolectric-"
          + frameworkSdkBuildVersion
          + "-"
          + preinstrumentedVersion;
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
        case "29":
          return API_29;
        case "30":
          return API_30;
        case "31":
          return API_31;
        case "32":
          return API_32;
        case "33":
          return API_33;
        default:
          throw new IllegalStateException("Unknown Robolectric API Level: " + apiLevel);
      }
    }
  }
}
