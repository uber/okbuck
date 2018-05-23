package com.uber.okbuck.core.model.android;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.api.ApplicationVariant;
import com.android.build.gradle.api.BaseVariant;
import com.android.builder.model.SigningConfig;
import com.android.manifmerger.ManifestMerger2;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.core.util.FileUtil;
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
import org.gradle.api.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** An Android app target */
public class AndroidAppTarget extends AndroidLibTarget {

  private static final Logger LOG = LoggerFactory.getLogger(AndroidAppTarget.class);
  private static final int DEFAULT_LINEARALLOC_LIMIT = 16777216;
  private final boolean multidexEnabled;
  private final Keystore keystore;
  private final Set<String> cpuFilters;
  private final int linearAllocHardLimit;
  private final List<String> primaryDexPatterns;
  private final List<String> exoPackageDependencies;
  private final File proguardMappingFile;
  private final boolean minifyEnabled;
  private final Map<String, Object> placeholders = new LinkedHashMap<>();
  private final boolean includesVectorDrawables;
  private final AndroidAppInstrumentationTarget appInstrumentationTarget;

  public AndroidAppTarget(Project project, String name, boolean isTest) {
    super(project, name, isTest);

    minifyEnabled = getBaseVariant().getBuildType().isMinifyEnabled();
    keystore = extractKeystore();

    final Set<String> filters = getBaseVariant().getNdkCompile().getAbiFilters();
    if (filters == null) {
      cpuFilters = ImmutableSet.of();
    } else {
      cpuFilters = filters;
    }

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
  public ManifestMerger2.MergeType getMergeType() {
    return ManifestMerger2.MergeType.APPLICATION;
  }

  @Override
  public boolean shouldGenerateBuildConfig() {
    // Always generate for apps
    return true;
  }

  @Override
  public Document processManifestXml(Document manifestXml) {
    String manifestPackage;
    if (getIsTest()) {
      manifestPackage = getApplicationId() + getApplicationIdSuffix() + ".test";
    } else {
      manifestPackage = getApplicationId() + getApplicationIdSuffix();
    }

    Element documentElement = manifestXml.getDocumentElement();

    documentElement.setAttribute("package", manifestPackage);
    documentElement.setAttribute("android:versionCode", String.valueOf(getVersionCode()));
    documentElement.setAttribute("android:versionName", getVersionName());

    NodeList nodeList = manifestXml.getElementsByTagName("application");
    Preconditions.checkArgument(nodeList.getLength() <= 1);
    if (nodeList.getLength() == 1) {
      Element applicationElement = (Element) nodeList.item(0);
      applicationElement.setAttribute("android:debuggable", String.valueOf(getDebuggable()));
    }

    return super.processManifestXml(manifestXml);
  }

  public ExoPackageScope getExopackage() {
    if (getProp(getOkbuck().exopackage, false)) {
      return new ExoPackageScope(getProject(), getMain(), exoPackageDependencies, getManifest());
    } else {
      return null;
    }
  }

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

  private Keystore extractKeystore() {
    SigningConfig config = getBaseVariant().getMergedFlavor().getSigningConfig();

    if (config == null) {
      config = getBaseVariant().getBuildType().getSigningConfig();
    }

    if (config != null) {
      return new Keystore(
          config.getStoreFile(),
          config.getKeyAlias(),
          config.getStorePassword(),
          config.getKeyPassword(),
          getGenPath());
    }
    return null;
  }

  public final boolean getMultidexEnabled() {
    return multidexEnabled;
  }

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

  public final AndroidAppInstrumentationTarget getAppInstrumentationTarget() {
    return appInstrumentationTarget;
  }

  public static class Keystore {

    Keystore(File storeFile, String alias, String storePassword, String keyPassword, File path) {
      this.storeFile = storeFile;
      this.alias = alias;
      this.storePassword = storePassword;
      this.keyPassword = keyPassword;
      this.path = path;
    }

    public final File getStoreFile() {
      return storeFile;
    }

    public final String getAlias() {
      return alias;
    }

    public final String getStorePassword() {
      return storePassword;
    }

    public final String getKeyPassword() {
      return keyPassword;
    }

    public final File getPath() {
      return path;
    }

    private final File storeFile;
    private final String alias;
    private final String storePassword;
    private final String keyPassword;
    private final File path;
  }
}
