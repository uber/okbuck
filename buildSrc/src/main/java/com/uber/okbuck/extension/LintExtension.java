package com.uber.okbuck.extension;

import com.uber.okbuck.core.annotation.Experimental;
import com.uber.okbuck.core.util.LintUtil;
import org.gradle.api.Project;

@Experimental
public class LintExtension {

  /** Lint jar version */
  @SuppressWarnings("CanBeFinal")
  public String version;

  /** Set to {@code true} to disable generation of lint rules */
  public boolean disabled = false;

  /** JVM arguments when invoking lint */
  public String jvmArgs = "-Xmx1024m";

  public LintExtension() {}

  public LintExtension(Project project) {
    version = LintUtil.getDefaultLintVersion(project);
  }
}
