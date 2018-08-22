package com.uber.okbuck.core.model.android;

import com.android.build.gradle.LibraryExtension;
import com.android.build.gradle.api.BaseVariant;
import com.android.build.gradle.api.LibraryVariant;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.core.model.base.ProjectType;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.TestExtension;
import java.io.File;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.gradle.api.Project;

/** An Android library target */
public class AndroidLibTarget extends AndroidTarget {

  @Nullable private final AndroidLibInstrumentationTarget libInstrumentationTarget;

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

  public boolean shouldGenerateBuildConfig() {
    return getOkbuck().libraryBuildConfig;
  }

  @Nullable
  public String getConsumerProguardConfig() {
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

  @Nullable
  public final AndroidLibInstrumentationTarget getLibInstrumentationTarget() {
    return libInstrumentationTarget;
  }
}
