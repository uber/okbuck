package com.uber.okbuck.extension;

import com.uber.okbuck.core.manager.KotlinHomeManager;
import javax.annotation.Nullable;
import org.gradle.api.Project;

public class KotlinExtension {

  /** Version of the kotlin compiler to use. */
  @Nullable public String version;

  KotlinExtension(Project project) {
    version = KotlinHomeManager.getDefaultKotlinVersion(project);
  }
}
