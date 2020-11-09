package com.uber.okbuck.extension;

import javax.annotation.Nullable;
import org.gradle.api.Project;

public class KotlinExtension {

  /** Version of the kotlin compiler to use. */
  @Nullable public String version;

  /** Sha256 of the compiler zip. */
  @Nullable public String compilerZipSha256;

  KotlinExtension(Project project) {
    this.version = "1.4.10";
    this.compilerZipSha256 = "bb1a21d70e521a01ae104e99a082a6e7bb58699b86347049da521d175d0dace7";
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
