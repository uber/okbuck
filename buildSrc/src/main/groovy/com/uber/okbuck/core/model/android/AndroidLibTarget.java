package com.uber.okbuck.core.model.android;

import com.android.build.gradle.LibraryExtension;
import com.android.build.gradle.api.BaseVariant;
import com.android.build.gradle.api.LibraryVariant;
import com.android.manifmerger.ManifestMerger2;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.core.model.base.ProjectType;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.KotlinUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.TestExtension;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import org.gradle.api.Project;
import org.xml.sax.SAXException;

/** An Android library target */
public class AndroidLibTarget extends AndroidTarget {

  private static final String KOTLIN_EXTENSIONS_OPTION = "plugin:org.jetbrains.kotlin.android:";
  private final AndroidLibInstrumentationTarget libInstrumentationTarget;

  public AndroidLibTarget(Project project, String name) {
    this(project, name, false);
  }

  public AndroidLibTarget(Project project, String name, boolean isTest) {
    super(project, name, isTest);

    TestExtension testExtension = getOkbuck().getTestExtension();

    // do not try to create this for android apps
    if (testExtension.espressoForLibraries
        && getInstrumentationTestVariant() != null
        && ProjectUtil.getType(project).equals(ProjectType.ANDROID_LIB)) {
      libInstrumentationTarget =
          new AndroidLibInstrumentationTarget(
              project, AndroidInstrumentationTarget.getInstrumentationTargetName(name));
    } else {
      libInstrumentationTarget = null;
    }
  }

  @Override
  protected BaseVariant getBaseVariant() {
    LibraryExtension libraryExtension = (LibraryExtension) getAndroidExtension();
    Optional<LibraryVariant> baseVariantOptional =
        libraryExtension
            .getLibraryVariants()
            .stream()
            .filter(variant -> variant.getName().equals(getName()))
            .findFirst();

    Preconditions.checkArgument(baseVariantOptional.isPresent());

    return baseVariantOptional.get();
  }

  @Override
  public ManifestMerger2.MergeType getMergeType() {
    return ManifestMerger2.MergeType.LIBRARY;
  }

  public boolean shouldGenerateBuildConfig() {
    return getOkbuck().libraryBuildConfig;
  }

  String getConsumerProguardConfig() {
    Set<File> consumerProguardFiles =
        new ImmutableSet.Builder<File>()
            .addAll(getBaseVariant().getMergedFlavor().getConsumerProguardFiles())
            .addAll(getBaseVariant().getBuildType().getConsumerProguardFiles())
            .build();

    if (consumerProguardFiles.size() > 0) {
      Optional<File> optionalFile = consumerProguardFiles.stream().findFirst();
      return FileUtil.getRelativePath(getProject().getProjectDir(), optionalFile.get());
    }
    return null;
  }

  public List<String> getKotlincArguments()
      throws ParserConfigurationException, ManifestMerger2.MergeFailureException, SAXException,
          IOException {
    if (!getHasKotlinAndroidExtensions()) {
      return ImmutableList.of();
    }

    ImmutableList.Builder<String> extraKotlincArgs = ImmutableList.builder();

    StringBuilder plugin = new StringBuilder();
    StringBuilder resDirs = new StringBuilder();
    StringBuilder options = new StringBuilder();

    // :root:module -> root/module/
    final String module =
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
    plugin.append(KotlinUtil.KOTLIN_LIBRARIES_LOCATION);
    plugin.append(File.separator);
    plugin.append("kotlin-android-extensions.jar");

    options.append(resDirs.toString());
    options.append(KOTLIN_EXTENSIONS_OPTION);
    options.append("package=");
    options.append(getPackage());

    if (getHasExperimentalKotlinAndroidExtensions()) {
      options.append(",");
      options.append(KOTLIN_EXTENSIONS_OPTION);
      options.append("experimental=true");
    }

    extraKotlincArgs.add(plugin.toString());
    extraKotlincArgs.add("-P");
    extraKotlincArgs.add(options.toString());

    return extraKotlincArgs.build();
  }

  public final AndroidLibInstrumentationTarget getLibInstrumentationTarget() {
    return libInstrumentationTarget;
  }
}
