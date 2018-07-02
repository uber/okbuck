package com.uber.okbuck.extension;

import com.uber.okbuck.core.manager.KotlinManager;
import org.gradle.api.Project;

public class KotlinExtension {

  /** Version of the kotlin compiler to use. */
  @SuppressWarnings("CanBeFinal")
  public String version;

  KotlinExtension(Project project) {
    version = KotlinManager.getDefaultKotlinVersion(project);
  }
}
