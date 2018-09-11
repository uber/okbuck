package com.uber.okbuck.core.model.android;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.api.ApplicationVariant;
import com.android.build.gradle.api.BaseVariant;
import com.android.build.gradle.tasks.NdkCompile;
import com.android.builder.model.SigningConfig;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.TestExtension;
import com.uber.okbuck.extension.TransformExtension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.gradle.api.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** An Android app target */
public class AndroidAppTarget extends AndroidLibTarget {

  private static final Logger LOG = LoggerFactory.getLogger(AndroidAppTarget.class);
  private static final int DEFAULT_LINEARALLOC_LIMIT = 16777216;
  private final boolean multidexEnabled;
  @Nullable private final Keystore keystore;
  private final Set<String> cpuFilters;
  private final int linearAllocHardLimit;
  private final List<String> primaryDexPatterns;
  private final List<String> exoPackageDependencies;
  private final File proguardMappingFile;
  private final boolean minifyEnabled;
  private final Map<String, Object> placeholders = new LinkedHashMap<>();
  private final boolean includesVectorDrawables;
  @Nullable private final AndroidAppInstrumentationTarget appInstrumentationTarget;

  public AndroidAppTarget(Project project, String name, boolean isTest) {
    super(project, name, isTest);

    minifyEnabled = getBaseVariant().getBuildType().isMinifyEnabled();
    keystore = extractKeystore();

    BaseVariant baseVariant = getBaseVariant();
    NdkCompile ndkCompile = baseVariant.getNdkCompile();

    Set<String> filters = ndkCompile != null ? ndkCompile.getAbiFilters() : ImmutableSet.of();
    cpuFilters = filters != null ? filters : ImmutableSet.of();

    Boolean multidex = getBaseVariant().getMergedFlavor().getMultiDexEnabled();
    if (multidex == null) {
      multidexEnabled = false;
    } else {
      multidexEnabled = multidex;
    }

    primaryDexPatterns = getProp(getOkbuck().primaryDexPatterns, ImmutableList.of());
    linearAllocHardLimit = getProp(getOkbuck().linearAllocHardLimit, DEFAULT_LINEARALLOC_LIMIT);
    exoPackageDependencies = getProp(getOkbuck().appLibDependencies, ImmutableList.of());
    proguardMappingFile = getProp(getOkbuck().proguardMappingFile, null);

    if (isTest) {
      placeholders.put(
          "applicationId", minus(getApplicationId(), ".test") + getApplicationIdSuffix() + ".test");
    } else {
      placeholders.put("applicationId", getApplicationId() + getApplicationIdSuffix());
    }

    placeholders.putAll(getBaseVariant().getBuildType().getManifestPlaceholders());
    placeholders.putAll(getBaseVariant().getMergedFlavor().getManifestPlaceholders());

    includesVectorDrawables =
        getAndroidExtension().getDefaultConfig().getVectorDrawables().getUseSupportLibrary();

    TestExtension testExtension = getOkbuck().getTestExtension();

    if (testExtension.espresso && getInstrumentationTestVariant() != null) {
      appInstrumentationTarget =
          new AndroidAppInstrumentationTarget(
              project, AndroidInstrumentationTarget.getInstrumentationTargetName(name));
    } else {
      appInstrumentationTarget = null;
    }
  }

  public AndroidAppTarget(Project project, String name) {
    this(project, name, false);
  }

  @Override
  protected BaseVariant getBaseVariant() {
    AppExtension appExtension = (AppExtension) getAndroidExtension();
    Optional<ApplicationVariant> optionalBaseVariant =
        appExtension
            .getApplicationVariants()
            .stream()
            .filter(variant -> variant.getName().equals(getName()))
            .findFirst();

    Preconditions.checkArgument(optionalBaseVariant.isPresent());

    return optionalBaseVariant.get();
  }

  @Override
  public boolean shouldGenerateBuildConfig() {
    // Always generate for apps
    return true;
  }

  @Nullable
  public ExoPackageScope getExopackage() {
    if (getProp(getOkbuck().exopackage, false)) {
      return new ExoPackageScope(
          getProject(), getMain(), exoPackageDependencies, getMainManifest());
    } else {
      return null;
    }
  }

  @Nullable
  public String getProguardConfig() {
    if (minifyEnabled) {
      Set<File> proguardFiles =
          new ImmutableSet.Builder<File>()
              .addAll(getBaseVariant().getMergedFlavor().getProguardFiles())
              .addAll(getBaseVariant().getBuildType().getProguardFiles())
              .build();

      Preconditions.checkArgument(
          proguardFiles.size() == 1,
          "%s proguard files found. Only one can be used.",
          proguardFiles.size());
      File proguardFile = proguardFiles.iterator().next();
      Preconditions.checkArgument(
          proguardFile.exists(), "Proguard file %s does not exist", proguardFile);
      File genProguardConfig = getGenPath("proguard.pro");
      try {
        LOG.info("Creating symlink {} -> {}", genProguardConfig, proguardFile);
        Files.createSymbolicLink(genProguardConfig.toPath(), proguardFile.toPath());
      } catch (IOException ignored) {
        LOG.info("Could not create symlink {} -> {}", genProguardConfig, proguardFile);
      }

      if (proguardMappingFile != null) {
        File genProguardMappingFile = getGenPath("proguard.map");
        try {
          LOG.info("Creating symlink {} -> {}", genProguardMappingFile, proguardMappingFile);
          Files.createSymbolicLink(genProguardMappingFile.toPath(), proguardMappingFile.toPath());
        } catch (IOException ignored) {
          LOG.info(
              "Could not create symlink {} -> {}", genProguardMappingFile, proguardMappingFile);
        }
      }

      return FileUtil.getRelativePath(getProject().getRootDir(), genProguardConfig);
    }

    return null;
  }

  @Nullable
  public String getProguardMapping() {
    if (proguardMappingFile == null) {
      return null;
    }

    return FileUtil.getRelativePath(getProject().getRootDir(), getGenPath("proguard.map"));
  }

  public List<Map<String, String>> getTransforms() {
    TransformExtension transform = getOkbuck().getTransformExtension();
    return getProp(transform.transforms, ImmutableList.of());
  }

  @Nullable
  private Keystore extractKeystore() {
    SigningConfig mergedConfig = getBaseVariant().getMergedFlavor().getSigningConfig();
    SigningConfig config =
        mergedConfig != null ? mergedConfig : getBaseVariant().getBuildType().getSigningConfig();

    if (config != null) {
      String keystoreFilePath =
          FileUtil.getRelativePath(getRootProject().getProjectDir(), config.getStoreFile());
      ProjectUtil.getPlugin(getProject()).exportedPaths.add(keystoreFilePath);
      return Keystore.create(
          keystoreFilePath,
          config.getKeyAlias(),
          config.getStorePassword(),
          config.getKeyPassword());
    }
    return null;
  }

  public final boolean getMultidexEnabled() {
    return multidexEnabled;
  }

  @Nullable
  public final Keystore getKeystore() {
    return keystore;
  }

  public final Set<String> getCpuFilters() {
    return cpuFilters;
  }

  public final int getLinearAllocHardLimit() {
    return linearAllocHardLimit;
  }

  public final List<String> getPrimaryDexPatterns() {
    return primaryDexPatterns;
  }

  public final boolean getMinifyEnabled() {
    return minifyEnabled;
  }

  public final Map<String, Object> getPlaceholders() {
    return placeholders;
  }

  public final boolean getIncludesVectorDrawables() {
    return includesVectorDrawables;
  }

  @Nullable
  public final AndroidAppInstrumentationTarget getAppInstrumentationTarget() {
    return appInstrumentationTarget;
  }
}
