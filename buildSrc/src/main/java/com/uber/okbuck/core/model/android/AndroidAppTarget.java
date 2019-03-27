package com.uber.okbuck.core.model.android;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.api.ApplicationVariant;
import com.android.build.gradle.api.BaseVariant;
import com.android.build.gradle.tasks.NdkCompile;
import com.android.builder.model.SigningConfig;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.model.base.Target;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.TestExtension;
import com.uber.okbuck.extension.TransformExtension;
import java.io.File;
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

    multidexEnabled =
        Optional.ofNullable(getBaseVariant().getBuildType().getMultiDexEnabled())
            .orElse(
                Optional.ofNullable(getBaseVariant().getMergedFlavor().getMultiDexEnabled())
                    .orElse(false));

    primaryDexPatterns = getProp(getOkbuck().primaryDexPatterns, ImmutableList.of());
    linearAllocHardLimit = getProp(getOkbuck().linearAllocHardLimit, DEFAULT_LINEARALLOC_LIMIT);
    exoPackageDependencies = getProp(getOkbuck().appLibDependencies, ImmutableList.of());
    proguardMappingFile = getProp(getOkbuck().proguardMappingFile, null);

    placeholders.put("applicationId", this.getApplicationIdWithSuffix());
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
  public Set<ExternalDependency> getApiExternalDeps() {
    // App targets don't have any deps to export
    return ImmutableSet.of();
  }

  @Override
  public Set<Target> getApiTargetDeps() {
    // App targets don't have any deps to export
    return ImmutableSet.of();
  }

  @Override
  public boolean shouldGenerateBuildConfig() {
    // Always generate for apps
    return true;
  }

  @Nullable
  public ExoPackageScope getExopackage() {
    if (getProp(getOkbuck().exopackage, false)) {
      return new ExoPackageScope(getProject(), getMain(), exoPackageDependencies, getExoManifest());
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

      return FileUtil.getRelativePath(getProject().getRootDir(), proguardFile);
    }

    return null;
  }

  @Nullable
  public String getProguardMapping() {
    if (!minifyEnabled || proguardMappingFile == null || !proguardMappingFile.exists()) {
      return null;
    }

    return FileUtil.getRelativePath(getProject().getRootDir(), proguardMappingFile);
  }

  public List<Map<String, String>> getTransforms() {
    TransformExtension transform = getOkbuck().getTransformExtension();
    return getProp(transform.transforms, ImmutableList.of());
  }

  @Override
  public String getPackage() {
    return getOriginalPackage();
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
          config.getStorePassword(),
          config.getKeyAlias(),
          config.getKeyPassword());
    }
    return null;
  }

  @Nullable
  private String getExoManifest() {
    String mainManifest = getMainManifest();
    List<String> secondaryManifests = getSecondaryManifests();

    if (secondaryManifests != null) {
      Optional<String> optionalExoManifest =
          secondaryManifests
              .stream()
              .filter(manifest -> manifest.contains("/" + getName() + "/"))
              .findAny();

      return optionalExoManifest.orElse(mainManifest);
    }

    return mainManifest;
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
