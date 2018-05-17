package com.uber.okbuck.core.dependency;

import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.core.util.FileUtil;
import java.io.File;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.UnknownConfigurationException;
import org.jetbrains.annotations.Nullable;

public final class DependencyUtils {

  private static final Set<String> ALLOWED_EXTENSIONS = ImmutableSet.of("jar", "aar", "pex");
  private static final Set<String> WHITELIST_LOCAL_PATTERNS =
      ImmutableSet.of("generated-gradle-jars/gradle-api-", "wrapper/dists");

  private DependencyUtils() {}

  @Nullable
  public static Configuration useful(Project project, String configuration) {
    try {
      Configuration config = project.getConfigurations().getByName(configuration);
      return useful(config);
    } catch (UnknownConfigurationException ignored) {
      return null;
    }
  }

  @Nullable
  public static Configuration useful(@Nullable Configuration configuration) {
    return configuration != null ? (configuration.isCanBeResolved() ? configuration : null) : null;
  }

  public static File createCacheDir(
      Project project, String cacheDirPath, @Nullable String buckFile) {
    File cacheDir = project.getRootProject().file(cacheDirPath);
    cacheDir.mkdirs();

    if (buckFile != null) {
      FileUtil.copyResourceToProject(buckFile, new File(cacheDir, "BUCK"));
    }

    return cacheDir;
  }

  public static File createCacheDir(Project project, String cacheDirPath) {
    return DependencyUtils.createCacheDir(project, cacheDirPath, null);
  }

  public static boolean isWhiteListed(final File depFile) {
    return WHITELIST_LOCAL_PATTERNS
        .stream()
        .anyMatch(pattern -> depFile.getPath().contains(pattern));
  }

  public static boolean isConsumable(File file) {
    return FilenameUtils.isExtension(file.getName(), ALLOWED_EXTENSIONS);
  }

  public static String getModuleVersion(String fileNameString, String version) {
    String baseFileName = FilenameUtils.getBaseName(fileNameString);
    if (baseFileName.length() > 0) {
      int versionIndex = fileNameString.lastIndexOf(version);
      if (versionIndex > -1) {
        return baseFileName.substring(versionIndex);
      } else {
        return version;
      }
    } else {
      throw new IllegalStateException(
          String.format("Not a valid module filename %s", fileNameString));
    }
  }
}
