package com.uber.okbuck.extension;

import javax.annotation.Nullable;
import org.gradle.api.Project;
import org.gradle.api.tasks.Internal;

public class KotlinExtension {

  /** Version of the kotlin compiler to use. */
  @Nullable public String version;

  /** Sha256 of the compiler zip. */
  @Nullable public String compilerZipSha256;

  KotlinExtension(Project project) {
    this.version = "1.5.31";
    this.compilerZipSha256 = "661111286f3e5ac06aaf3a9403d869d9a96a176b62b141814be626a47249fe9e";
  }

  @Internal
  public String getCompilerZipDownloadUrl() {
    return String.format(
        "https://github.com/JetBrains/kotlin/releases/download/v%s/kotlin-compiler-%s.zip",
        this.version, this.version);
  }

  @Nullable
  @Internal
  public String getCompilerZipSha256() {
    return this.compilerZipSha256;
  }
}
