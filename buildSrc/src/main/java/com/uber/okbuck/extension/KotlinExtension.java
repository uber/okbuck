package com.uber.okbuck.extension;

import javax.annotation.Nullable;
import org.gradle.api.Project;

public class KotlinExtension {

  /** Version of the kotlin compiler to use. */
  @Nullable public String version;

  /** Sha256 of the compiler zip. */
  @Nullable public String compilerZipSha256;

  KotlinExtension(Project project) {
    this.version = "3.72.0";
    this.compilerZipSha256 = "ccd0db87981f1c0e3f209a1a4acb6778f14e63fe3e561a98948b5317e526cc6c";
  }

  public String getCompilerZipDownloadUrl() {
    return String.format(
        "https://github.com/JetBrains/kotlin/releases/download/v%s/kotlin-compiler-%s.zip",
        this.version, this.version);
  }

  @Nullable
  public String getCompilerZipSha256() {
    return this.compilerZipSha256;
  }
}
