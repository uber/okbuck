package com.uber.okbuck.core.dependency;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.core.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.UnknownConfigurationException;
import org.gradle.api.file.FileTree;

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
    if (configuration != null && configuration.isCanBeResolved()) {
      return configuration;
    }
    return null;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public static boolean isWhiteListed(final File dependencyFile) {
    return WHITELIST_LOCAL_PATTERNS
        .stream()
        .anyMatch(pattern -> dependencyFile.getPath().contains(pattern));
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public static boolean isConsumable(File file) {
    // Skip artifact files which are coming from the transformed folder.
    // transforms-1 contains the contents of the resolved aar/jar and
    // hence should not be consumed.
    if (file.getAbsolutePath().contains("transforms-1/files-1")) {
      return false;
    }
    return FilenameUtils.isExtension(file.getName(), ALLOWED_EXTENSIONS);
  }

  @Nullable
  static String getModuleClassifier(String fileNameString, @Nullable String version) {
    if (version == null) {
      return null;
    }

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

  @Nullable
  static Path getContentPath(Path zipFilePath, String contentFileName) {
    try {
      FileSystem zipFile = FileSystems.newFileSystem(zipFilePath, null);
      Path packagedPath = zipFile.getPath(contentFileName);
      if (Files.exists(packagedPath)) {
        return packagedPath;
      } else {
        return null;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Nullable
  static Path getSingleZipFilePath(Project project, File baseDir, String zipToFind) {
    FileTree zipFiles =
        project.fileTree(
            ImmutableMap.of(
                "dir", baseDir.getAbsolutePath(), "includes", ImmutableList.of("**/" + zipToFind)));

    try {
      File maybeZipFile = zipFiles.getSingleFile();
      if (FileUtil.isZipFile(maybeZipFile)) {
        return maybeZipFile.toPath();
      }
    } catch (IllegalStateException ignored) {
      if (zipFiles.getFiles().size() > 1) {
        throw new IllegalStateException("Found multiple source jars: " + zipFiles);
      }
    }
    return null;
  }
}
