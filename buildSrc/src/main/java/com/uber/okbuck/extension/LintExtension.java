package com.uber.okbuck.extension;

import com.uber.okbuck.core.annotation.Experimental;
import com.uber.okbuck.core.manager.LintManager;
import javax.annotation.Nullable;
import org.gradle.api.Project;

@Experimental
public class LintExtension {

  /** Lint jar version */
  @Nullable public String version;

  /** Set to {@code true} to disable generation of lint rules */
  public boolean disabled = false;

  /** JVM arguments when invoking lint */
  public String jvmArgs = "-Xmx1024m";

  /** Classpath entries matching regex to exclude during lint */
  @Nullable public String classpathExclusionRegex = null;

  LintExtension(Project project) {
    version = LintManager.getDefaultLintVersion(project);
  }
}
