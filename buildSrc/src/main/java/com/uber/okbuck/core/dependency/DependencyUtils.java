package com.uber.okbuck.core.dependency;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.extension.ExternalDependenciesExtension;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.UnknownConfigurationException;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.artifacts.ivyservice.DefaultLenientConfiguration;

public final class DependencyUtils {

  private static final ImmutableSet<String> ALLOWED_EXTENSIONS =
      ImmutableSet.of("jar", "aar", "pex");
  private static final ImmutableSet<String> WHITELIST_LOCAL_PATTERNS =
      ImmutableSet.of("generated-gradle-jars/gradle-api-", "wrapper/dists");

  private DependencyUtils() {}

  @Nullable
  public static Configuration useful(String configuration, Project project) {
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
  public static boolean isWhiteListed(File dependencyFile) {
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
          return Strings.emptyToNull(classifierSuffix.substring(1));
        } else if (classifierSuffix.length() > 0) {
          throw new IllegalStateException(
              String.format(
                  "Classifier doesn't have a delimiter: %s -- %s", fileNameString, version));
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

  public static Set<ExternalDependency> resolveExternal(
      Configuration configuration, ExternalDependenciesExtension extension) {
    try {
      return configuration
          .getIncoming()
          .getArtifacts()
          .getArtifacts()
          .stream()
          .map(
              artifact -> {
                ComponentIdentifier identifier = artifact.getId().getComponentIdentifier();
                if (identifier instanceof ProjectComponentIdentifier
                    || !isConsumable(artifact.getFile())) {
                  return null;
                }
                if (identifier instanceof ModuleComponentIdentifier
                    && ((ModuleComponentIdentifier) identifier).getVersion().length() > 0) {
                  ModuleComponentIdentifier moduleIdentifier =
                      (ModuleComponentIdentifier) identifier;
                  return ExternalDependency.from(
                      moduleIdentifier.getGroup(),
                      moduleIdentifier.getModule(),
                      moduleIdentifier.getVersion(),
                      artifact.getFile(),
                      extension);
                } else {
                  return ExternalDependency.fromLocal(artifact.getFile(), extension);
                }
              })
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());
    } catch (DefaultLenientConfiguration.ArtifactResolveException e) {
      throw artifactResolveException(e);
    }
  }

  private static IllegalStateException artifactResolveException(Exception e) {
    return new IllegalStateException(
        "Failed to resolve an artifact. Make sure you have a repositories block defined. "
            + "See https://github.com/uber/okbuck/wiki/Known-caveats#could-not-resolve-all-"
            + "dependencies-for-configuration for more information.",
        e);
  }
}
