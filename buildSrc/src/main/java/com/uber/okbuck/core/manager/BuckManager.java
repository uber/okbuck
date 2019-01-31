package com.uber.okbuck.core.manager;

import com.google.common.base.Preconditions;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.OkBuckExtension;
import java.io.File;
import java.nio.file.Path;
import java.util.Set;
import javax.annotation.Nullable;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

public final class BuckManager {

  private static final String BUCK_BINARY_LOCATION =
      OkBuckGradlePlugin.WORKSPACE_PATH + "/buck_binary";

  private static final String BUCK_BINARY_CONFIGURATION = "buckBinary";
  private static final String JITPACK_URL = "https://jitpack.io";

  private final Project rootProject;

  @Nullable private Path realBuckBinaryPath;

  public BuckManager(Project rootProject) {
    this.rootProject = rootProject;
  }

  public void setupBuckBinary() {
    OkBuckExtension okbuckExt = ProjectUtil.getOkBuckExtension(rootProject);

    // Create dependency cache for buck binary if needed
    if (okbuckExt.buckBinary != null) {
      Configuration buckConfig =
          rootProject.getConfigurations().maybeCreate(BUCK_BINARY_CONFIGURATION);
      rootProject
          .getRepositories()
          .maven(mavenArtifactRepository -> mavenArtifactRepository.setUrl(JITPACK_URL));
      rootProject.getDependencies().add(BUCK_BINARY_CONFIGURATION, okbuckExt.buckBinary);

      Set<File> resolvedFiles = buckConfig.getResolvedConfiguration().getFiles();
      Preconditions.checkArgument(resolvedFiles.size() == 1);
      realBuckBinaryPath = resolvedFiles.iterator().next().toPath();
    }
  }

  public void finalizeDependencies() {
    Path buckBinaryCache = rootProject.file(BUCK_BINARY_LOCATION).toPath();
    // Delete already existing folder
    FileUtil.deleteQuietly(buckBinaryCache);

    if (realBuckBinaryPath != null) {
      Path linkedBinaryPath = buckBinaryCache.resolve(realBuckBinaryPath.getFileName());

      // Make dirs
      linkedBinaryPath.getParent().toFile().mkdirs();

      FileUtil.symlink(linkedBinaryPath, realBuckBinaryPath);
    }
  }
}
