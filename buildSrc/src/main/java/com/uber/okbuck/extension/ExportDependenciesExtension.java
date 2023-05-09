package com.uber.okbuck.extension;

import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

import java.nio.file.Paths;

public class ExportDependenciesExtension {
  @Input private boolean enabled = false;
  @Input @Optional private String file = ".okbuck/raw-deps";

  private final String projectRoot;

  public ExportDependenciesExtension(Project project) {
    projectRoot = project.getProjectDir().getAbsolutePath();
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getFile() {
    return Paths.get(projectRoot, file).toString();
  }
}
