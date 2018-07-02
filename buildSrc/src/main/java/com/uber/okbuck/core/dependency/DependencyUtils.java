package com.uber.okbuck.core.dependency;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.OkBuckGradlePlugin;
import java.io.File;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.UnknownConfigurationException;

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

  public static File createCacheDir(Project project) {
    File cacheDir = project.getRootProject().file(OkBuckGradlePlugin.EXTERNAL_DEPENDENCY_CACHE);
    cacheDir.mkdirs();
    return cacheDir;
  }

  public static boolean isWhiteListed(final File depFile) {
    return WHITELIST_LOCAL_PATTERNS
        .stream()
        .anyMatch(pattern -> depFile.getPath().contains(pattern));
  }

  public static boolean isConsumable(File file) {
    return FilenameUtils.isExtension(file.getName(), ALLOWED_EXTENSIONS);
  }

  @Nullable
  public static String getModuleClassifier(String fileNameString, String version) {
    String baseFileName = FilenameUtils.getBaseName(fileNameString);
    if (baseFileName.length() > 0) {
      int versionIndex = fileNameString.lastIndexOf(version);
      if (versionIndex > -1) {
        String classifierSuffix = baseFileName.substring(versionIndex + version.length());
        if (classifierSuffix.startsWith("-")) {
          classifierSuffix = classifierSuffix.substring(1);
        }
        return Strings.emptyToNull(classifierSuffix);
      } else {
        return null;
      }
    } else {
      throw new IllegalStateException(
          String.format("Not a valid module filename %s", fileNameString));
    }
  }
}
