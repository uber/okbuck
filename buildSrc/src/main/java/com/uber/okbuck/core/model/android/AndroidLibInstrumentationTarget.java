package com.uber.okbuck.core.model.android;

import com.android.build.gradle.LibraryExtension;
import com.android.build.gradle.api.BaseVariant;
import com.android.build.gradle.api.LibraryVariant;
import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.gradle.api.Project;

/** An Android library instrumentation target */
public class AndroidLibInstrumentationTarget extends AndroidInstrumentationTarget {

  AndroidLibInstrumentationTarget(Project project, String name) {
    super(project, name);
  }

  @Override
  protected BaseVariant getBaseVariant() {
    return getLibraryVariant().getTestVariant();
  }

  @Override
  Map<String, String> getResVariantDirs() {
    Map<String, String> variantDirs = new HashMap<>();

    getLibraryVariant()
        .getSourceSets()
        .forEach(
            provider ->
                getAvailable(provider.getResDirectories())
                    .forEach(dir -> variantDirs.put(dir, provider.getName())));

    return variantDirs;
  }

  private LibraryVariant getLibraryVariant() {
    LibraryExtension libraryExtension = (LibraryExtension) getAndroidExtension();
    Set<LibraryVariant> libraryVariants =
        libraryExtension
            .getLibraryVariants()
            .matching(
                libraryVariant -> libraryVariant.getName().equals(getMainTargetName(getName())));

    Preconditions.checkArgument(libraryVariants.size() > 0);

    return libraryVariants.iterator().next();
  }
}
