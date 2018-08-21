package com.uber.okbuck.extension;

import com.uber.okbuck.core.manager.KotlinManager;
import org.gradle.api.Project;

import javax.annotation.Nullable;

public class KotlinExtension {

  /** Version of the kotlin compiler to use. */
  @SuppressWarnings("CanBeFinal")
  @Nullable public String version;

  KotlinExtension(Project project) {
    version = KotlinManager.getDefaultKotlinVersion(project);
  }
}
