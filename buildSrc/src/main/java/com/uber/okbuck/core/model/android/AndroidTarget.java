package com.uber.okbuck.core.model.android;

import static com.uber.okbuck.core.manager.KotlinManager.KOTLIN_ANDROID_EXTENSIONS_MODULE;
import static com.uber.okbuck.core.manager.KotlinManager.KOTLIN_KAPT_PLUGIN;

import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.api.BaseVariant;
import com.android.build.gradle.api.TestVariant;
import com.android.build.gradle.api.UnitTestVariant;
import com.android.build.gradle.internal.api.TestedVariant;
import com.android.builder.core.VariantType;
import com.android.builder.model.ClassField;
import com.android.builder.model.LintOptions;
import com.android.builder.model.SourceProvider;
import com.facebook.infer.annotation.Initializer;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Var;
import com.uber.okbuck.core.annotation.AnnotationProcessorCache;
import com.uber.okbuck.core.manager.KotlinManager;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.core.model.jvm.JvmTarget;
import com.uber.okbuck.core.model.jvm.TestOptions;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.core.util.XmlUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.jetbrains.kotlin.gradle.internal.AndroidExtensionsExtension;
import org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper;
import org.w3c.dom.Document;

/** An Android target */
public abstract class AndroidTarget extends JvmTarget {

  private static final String KOTLIN_EXTENSIONS_OPTION = "plugin:org.jetbrains.kotlin.android:";

  private static final String DEFAULT_SDK = "1";
  private final String applicationId;
  private final String applicationIdSuffix;
  private final String versionName;
  private final Integer versionCode;
  private final String minSdk;
  private final String targetSdk;
  private final boolean debuggable;
  private final boolean generateR2;
  private final boolean isKotlinAndroid;
  private final boolean isKapt;
  private final boolean hasKotlinAndroidExtensions;
  private final boolean hasExperimentalKotlinAndroidExtensions;
  private final boolean lintExclude;
  private final boolean testExclude;
  private final boolean isTest;

  @Nullable private String mainManifest;
  @Nullable private List<String> secondaryManifests;
  @Nullable private String originalPackageName;
  @Nullable private String resourceUnionPackageName;

  public AndroidTarget(Project project, String name, boolean isTest) {
    super(project, name);

    this.isTest = isTest;

    applicationIdSuffix =
        Strings.nullToEmpty(getBaseVariant().getMergedFlavor().getApplicationIdSuffix())
            + Strings.nullToEmpty(getBaseVariant().getBuildType().getApplicationIdSuffix());

    if (isTest) {
      String applicationIdString =
          minus(minus(getBaseVariant().getApplicationId(), ".test"), applicationIdSuffix);
      applicationId = minus(applicationIdString, applicationIdSuffix);
    } else {
      applicationId = minus(getBaseVariant().getApplicationId(), applicationIdSuffix);
    }

    versionName = getBaseVariant().getMergedFlavor().getVersionName();
    versionCode = getBaseVariant().getMergedFlavor().getVersionCode();

    debuggable = getBaseVariant().getBuildType().isDebuggable();

    // Butterknife support
    generateR2 = project.getPlugins().hasPlugin("com.jakewharton.butterknife");

    // Check if kotlin
    isKotlinAndroid = project.getPlugins().hasPlugin(KotlinAndroidPluginWrapper.class);
    isKapt = project.getPlugins().hasPlugin(KOTLIN_KAPT_PLUGIN);
    hasKotlinAndroidExtensions = project.getPlugins().hasPlugin(KOTLIN_ANDROID_EXTENSIONS_MODULE);

    // Check if any rules are excluded
    lintExclude = getProp(getOkbuck().lintExclude, ImmutableList.of()).contains(name);
    testExclude = getProp(getOkbuck().testExclude, ImmutableList.of()).contains(name);

    resourceUnionPackageName = getOkbuck().resourceUnionPackage;

    @Var boolean hasKotlinExtension;
    try {
      AndroidExtensionsExtension androidExtensions =
          project.getExtensions().getByType(AndroidExtensionsExtension.class);
      hasKotlinExtension = hasKotlinAndroidExtensions && androidExtensions.isExperimental();
    } catch (Exception ignored) {
      hasKotlinExtension = false;
    }
    hasExperimentalKotlinAndroidExtensions = hasKotlinExtension;

    if (getBaseVariant().getMergedFlavor().getMinSdkVersion() == null
        || getBaseVariant().getMergedFlavor().getTargetSdkVersion() == null) {
      minSdk = targetSdk = DEFAULT_SDK;
      throw new IllegalStateException(
          "module `"
              + project.getName()
              + "` must specify minSdkVersion and targetSdkVersion in build.gradle");
    } else {
      minSdk = getBaseVariant().getMergedFlavor().getMinSdkVersion().getApiString();
      targetSdk = getBaseVariant().getMergedFlavor().getTargetSdkVersion().getApiString();
    }
  }

  public AndroidTarget(Project project, String name) {
    this(project, name, false);
  }

  protected abstract BaseVariant getBaseVariant();

  @Override
  public Scope getMain() {
    return Scope.builder(getProject())
        .configuration(getBaseVariant().getRuntimeConfiguration())
        .sourceDirs(getSources(getBaseVariant()))
        .javaResourceDirs(getJavaResources(getBaseVariant()))
        .customOptions(JAVA_COMPILER_EXTRA_ARGUMENTS, getJavaCompilerOptions(getBaseVariant()))
        .customOptions(KOTLIN_COMPILER_EXTRA_ARGUMENTS, getKotlinCompilerOptions())
        .customOptions(getKotlinFriendPaths(false))
        .build();
  }

  @Override
  public Scope getTest() {
    Scope.Builder builder = Scope.builder(getProject());
    UnitTestVariant unitTestVariant = getUnitTestVariant();
    if (unitTestVariant != null) {
      builder.configuration(unitTestVariant.getRuntimeConfiguration());
      builder.sourceDirs(getSources(unitTestVariant));
      builder.javaResourceDirs(getJavaResources(unitTestVariant));
      builder.customOptions(JAVA_COMPILER_EXTRA_ARGUMENTS, getJavaCompilerOptions(unitTestVariant));
      builder.customOptions(KOTLIN_COMPILER_EXTRA_ARGUMENTS, getKotlinCompilerOptions());
      builder.customOptions(getKotlinFriendPaths(true));
    }
    return builder.build();
  }

  @Override
  public List<Scope> getAptScopes() {
    Configuration configuration = getAptConfigurationFromVariant(getBaseVariant());
    AnnotationProcessorCache apCache = ProjectUtil.getAnnotationProcessorCache(getProject());
    return configuration != null
        ? apCache.getAnnotationProcessorScopes(getProject(), configuration)
        : ImmutableList.of();
  }

  @Override
  public List<Scope> getTestAptScopes() {
    Configuration configuration = getAptConfigurationFromVariant(getUnitTestVariant());
    AnnotationProcessorCache apCache = ProjectUtil.getAnnotationProcessorCache(getProject());
    return configuration != null
        ? apCache.getAnnotationProcessorScopes(getProject(), configuration)
        : ImmutableList.of();
  }

  @Override
  public Scope getApt() {
    Configuration configuration = getAptConfigurationFromVariant(getBaseVariant());
    return configuration != null
        ? getAptScopeForConfiguration(configuration)
        : Scope.builder(getProject()).build();
  }

  @Override
  public Scope getTestApt() {
    Configuration configuration = getAptConfigurationFromVariant(getUnitTestVariant());
    return configuration != null
        ? getAptScopeForConfiguration(configuration)
        : Scope.builder(getProject()).build();
  }

  @Override
  public Scope getProvided() {
    return Scope.builder(getProject())
        .configuration(getBaseVariant().getCompileConfiguration())
        .build();
  }

  @Override
  public Scope getTestProvided() {
    return Scope.builder(getProject())
        .configuration(
            getUnitTestVariant() != null ? getUnitTestVariant().getCompileConfiguration() : null)
        .build();
  }

  @Override
  public LintOptions getLintOptions() {
    return getAndroidExtension().getLintOptions();
  }

  public boolean getRobolectricEnabled() {
    return getOkbuck().getTestExtension().robolectric && !testExclude;
  }

  public boolean getLintEnabled() {
    return !getOkbuck().getLintExtension().disabled && !lintExclude;
  }

  @Override
  public String getSourceCompatibility() {
    return JvmTarget.javaVersion(
        getAndroidExtension().getCompileOptions().getSourceCompatibility());
  }

  @Override
  public String getTargetCompatibility() {
    return JvmTarget.javaVersion(
        getAndroidExtension().getCompileOptions().getTargetCompatibility());
  }

  @Override
  public TestOptions getTestOptions() {
    String testTaskName =
        VariantType.UNIT_TEST_PREFIX
            + StringUtils.capitalize(getName())
            + VariantType.UNIT_TEST_SUFFIX;
    List<Test> testTasks = ImmutableList.copyOf(getProject().getTasks().withType(Test.class));
    Optional<Test> optionalTest =
        testTasks.stream().filter(test -> test.getName().equals(testTaskName)).findFirst();

    List<String> jvmArgs =
        optionalTest.map(Test::getAllJvmArgs).orElseGet(Collections::<String>emptyList);
    Map<String, Object> env =
        optionalTest.map(Test::getEnvironment).orElseGet(Collections::emptyMap);

    System.getenv().keySet().forEach(env::remove);

    return new TestOptions(jvmArgs, env);
  }

  public List<String> getBuildConfigFields() {
    List<String> buildConfig = new ArrayList<>();

    buildConfig.add(String.format("String APPLICATION_ID = \"%s\"", getApplicationIdWithSuffix()));

    buildConfig.add(String.format("String BUILD_TYPE = \"%s\"", getBuildType()));
    buildConfig.add(String.format("String FLAVOR = \"%s\"", getFlavor()));

    if (versionCode != null) {
      buildConfig.add(String.format("int VERSION_CODE = %s", versionCode));
    }
    if (versionName != null) {
      buildConfig.add(String.format("String VERSION_NAME = \"%s\"", versionName));
    }

    Map<String, ClassField> extraBuildConfig = new HashMap<>();
    extraBuildConfig.putAll(getBaseVariant().getMergedFlavor().getBuildConfigFields());
    extraBuildConfig.putAll(getBaseVariant().getBuildType().getBuildConfigFields());

    buildConfig.addAll(
        extraBuildConfig
            .keySet()
            .stream()
            .sorted()
            .map(
                key -> {
                  ClassField classField = extraBuildConfig.get(key);
                  if (classField == null) {
                    throw new IllegalStateException("Invalid buildconfig value!");
                  }
                  return String.format(
                      "%s %s = %s", classField.getType(), key, classField.getValue());
                })
            .collect(Collectors.toList()));

    return buildConfig;
  }

  String getFlavor() {
    return getBaseVariant().getFlavorName();
  }

  String getBuildType() {
    return getBaseVariant().getBuildType().getName();
  }

  public Set<String> getResDirs() {
    return getBaseVariant()
        .getSourceSets()
        .stream()
        .map(i -> getAvailable(i.getResDirectories()))
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  @Nullable
  public String getProjectResDir() {
    List<SourceProvider> sourceSets = getBaseVariant().getSourceSets();
    if (!sourceSets.isEmpty()) {
      SourceProvider main = sourceSets.get(0);
      Set<String> mainResDirectories = getAvailable(main.getResDirectories());
      if (mainResDirectories.size() > 0) {
        return mainResDirectories.iterator().next();
      }
    }
    return null;
  }

  /** Returns a map of each resource directory to its corresponding variant */
  Map<String, String> getResVariantDirs() {
    Map<String, String> variantDirs = new HashMap<>();
    for (SourceProvider provider : getBaseVariant().getSourceSets()) {
      for (String dir : getAvailable(provider.getResDirectories())) {
        variantDirs.put(dir, provider.getName());
      }
    }
    return variantDirs;
  }

  public Set<String> getAssetDirs() {
    return getBaseVariant()
        .getSourceSets()
        .stream()
        .map(i -> getAvailable(i.getAssetsDirectories()))
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  public Set<String> getAidl() {
    return getBaseVariant()
        .getSourceSets()
        .stream()
        .map(i -> getAvailable(i.getAidlDirectories()))
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  public Set<String> getJniLibs() {
    return getBaseVariant()
        .getSourceSets()
        .stream()
        .map(i -> getAvailable(i.getJniLibsDirectories()))
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  @Nullable
  public String getMainManifest() {
    if (mainManifest == null) {
      ensureManifest();
    }

    return mainManifest;
  }

  @Nullable
  public List<String> getSecondaryManifests() {
    return secondaryManifests;
  }

  @Initializer
  @SuppressWarnings("NullAway")
  void ensureManifest() {
    Set<String> manifests =
        getBaseVariant()
            .getSourceSets()
            .stream()
            .map(SourceProvider::getManifestFile)
            .map(file -> getAvailable(ImmutableSet.of(file)))
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    if (manifests.isEmpty()) {
      return;
    }

    secondaryManifests = new ArrayList<>(manifests);
    mainManifest = secondaryManifests.remove(0);
  }

  static List<String> getJavaCompilerOptions(BaseVariant baseVariant) {
    if (baseVariant != null && baseVariant.getJavaCompiler() instanceof JavaCompile) {
      List<String> options =
          ((JavaCompile) baseVariant.getJavaCompiler()).getOptions().getCompilerArgs();

      // Remove options added by apt plugin since they are handled by apt scope separately
      filterOptions(options, ImmutableList.of("-s", "-processorpath"));
      return options;
    } else {
      return ImmutableList.of();
    }
  }

  @Override
  protected List<String> getKotlinCompilerOptions() {
    if (!getHasKotlinAndroidExtensions()) {
      return super.getKotlinCompilerOptions();
    }

    ImmutableList.Builder<String> extraKotlincArgs = ImmutableList.builder();

    StringBuilder plugin = new StringBuilder();
    StringBuilder resDirs = new StringBuilder();
    StringBuilder options = new StringBuilder();

    // :root:module -> root/module/
    String module =
        getProject().getPath().replace(":", File.separator).substring(1) + File.separator;

    getResVariantDirs()
        .forEach(
            (String dir, String variant) -> {
              String pathToRes = module + dir;
              resDirs.append(KOTLIN_EXTENSIONS_OPTION);
              resDirs.append("variant=");
              resDirs.append(variant);
              resDirs.append(";");
              resDirs.append(pathToRes);
              resDirs.append(",");
            });

    plugin.append("-Xplugin=");
    plugin.append(KotlinManager.KOTLIN_LIBRARIES_LOCATION);
    plugin.append(File.separator);
    plugin.append("kotlin-android-extensions.jar");

    options.append(resDirs.toString());
    options.append(KOTLIN_EXTENSIONS_OPTION);
    options.append("package=");
    options.append(getResPackage());

    if (getHasExperimentalKotlinAndroidExtensions()) {
      options.append(",");
      options.append(KOTLIN_EXTENSIONS_OPTION);
      options.append("experimental=true");
    }

    extraKotlincArgs.add(plugin.toString());
    extraKotlincArgs.add("-P");
    extraKotlincArgs.add(options.toString());

    extraKotlincArgs.addAll(super.getKotlinCompilerOptions());

    return extraKotlincArgs.build();
  }

  static void filterOptions(List<String> options, List<String> remove) {
    remove.forEach(
        key -> {
          int index = options.indexOf(key);
          if (index != -1) {
            options.remove(index + 1);
            options.remove(index);
          }
        });
  }

  @Nullable
  private UnitTestVariant getUnitTestVariant() {
    if (getBaseVariant() instanceof TestedVariant) {
      return ((TestedVariant) getBaseVariant()).getUnitTestVariant();
    } else {
      return null;
    }
  }

  @Nullable
  TestVariant getInstrumentationTestVariant() {
    if (getBaseVariant() instanceof TestedVariant) {
      TestVariant testVariant = ((TestedVariant) getBaseVariant()).getTestVariant();
      if (testVariant != null) {
        Set<String> manifests = new HashSet<>();
        testVariant
            .getSourceSets()
            .forEach(
                provider ->
                    manifests.addAll(getAvailable(ImmutableSet.of(provider.getManifestFile()))));
        return manifests.isEmpty() ? null : testVariant;
      }
    }
    return null;
  }

  public RuleType getRuleType() {
    if (isKotlin()) {
      return RuleType.KOTLIN_ANDROID_MODULE;
    } else {
      return RuleType.ANDROID_MODULE;
    }
  }

  public RuleType getTestRuleType() {
    if (isKotlin()) {
      return RuleType.KOTLIN_ROBOLECTRIC_TEST;
    } else {
      return RuleType.ROBOLECTRIC_TEST;
    }
  }

  public boolean isKotlin() {
    return isKotlinAndroid;
  }

  @Nullable
  private Configuration getAptConfigurationFromVariant(@Nullable BaseVariant variant) {
    @Var Configuration configuration = null;
    if (isKapt) {
      configuration =
          getProject()
              .getConfigurations()
              .getByName("kapt" + StringUtils.capitalize(getBaseVariant().getName()));
    } else if (variant != null) {
      configuration = variant.getAnnotationProcessorConfiguration();
    }
    return configuration;
  }

  public BaseExtension getAndroidExtension() {
    return (BaseExtension) getProject().getExtensions().getByName("android");
  }

  public Set<File> getSources(BaseVariant variant) {
    ImmutableSet.Builder<File> srcs = new ImmutableSet.Builder<>();

    Set<File> javaSrcs =
        variant
            .getSourceSets()
            .stream()
            .map(SourceProvider::getJavaDirectories)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

    srcs.addAll(javaSrcs);

    if (isKotlinAndroid) {
      srcs.addAll(
          javaSrcs
              .stream()
              .filter(i -> i.getName().equals("java"))
              .map(i -> getProject().file(i.getAbsolutePath().replaceFirst("/java$", "/kotlin")))
              .collect(Collectors.toSet()));
    }
    return srcs.build();
  }

  public Set<File> getJavaResources(BaseVariant variant) {
    return variant
        .getSourceSets()
        .stream()
        .map(SourceProvider::getResourcesDirectories)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  String getOriginalPackage() {
    if (originalPackageName == null) {
      Document manifestXml = XmlUtil.loadXml(getProject().file(getMainManifest()));
      originalPackageName = manifestXml.getDocumentElement().getAttribute("package").trim();
    }
    return originalPackageName;
  }

  final String getApplicationIdSuffix() {
    return applicationIdSuffix;
  }

  final String getApplicationIdBase() {
    return applicationId;
  }

  public String getApplicationIdWithSuffix() {
    if (getIsTest()) {
      return minus(getApplicationIdBase(), ".test") + getApplicationIdSuffix() + ".test";
    } else {
      return getApplicationIdBase() + getApplicationIdSuffix();
    }
  }

  public String getPackage() {
    return getApplicationIdBase();
  }

  public String getResPackage() {
    if (resourceUnionPackageName != null) {
      return resourceUnionPackageName;
    } else {
      return getOriginalPackage();
    }
  }

  public final String getMinSdk() {
    return minSdk;
  }

  public final String getTargetSdk() {
    return targetSdk;
  }

  public final String getVersionName() {
    return versionName;
  }

  public final Integer getVersionCode() {
    return versionCode;
  }

  public final boolean getDebuggable() {
    return debuggable;
  }

  public final boolean getGenerateR2() {
    return generateR2;
  }

  final boolean getIsKapt() {
    return isKapt;
  }

  final boolean getHasKotlinAndroidExtensions() {
    return hasKotlinAndroidExtensions;
  }

  final boolean getHasExperimentalKotlinAndroidExtensions() {
    return hasExperimentalKotlinAndroidExtensions;
  }

  public boolean getIsTest() {
    return isTest;
  }

  static String minus(String s, String text) {
    int index = s.indexOf(text);
    if (index == -1) {
      return s;
    } else {
      int end = index + text.length();
      return s.length() > end ? s.substring(0, index) + s.substring(end) : s.substring(0, index);
    }
  }

  @Override
  public <T> T getProp(Map<String, T> map, @Nullable T defaultValue) {
    String nameKey = getIdentifier() + StringUtils.capitalize(getName());
    String flavorKey = getIdentifier() + StringUtils.capitalize(getFlavor());
    String buildTypeKey = getIdentifier() + StringUtils.capitalize(getBuildType());

    if (map.containsKey(nameKey)) {
      return map.get(nameKey);
    } else if (map.containsKey(flavorKey)) {
      return map.get(flavorKey);
    } else if (map.containsKey(buildTypeKey)) {
      return map.get(buildTypeKey);
    } else {
      return map.getOrDefault(getIdentifier(), defaultValue);
    }
  }
}
