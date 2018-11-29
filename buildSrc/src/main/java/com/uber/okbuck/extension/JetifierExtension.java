package com.uber.okbuck.extension;

import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.core.dependency.BaseExternalDependency;
import com.uber.okbuck.core.manager.JetifierManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.gradle.api.Project;

public class JetifierExtension {

  public static final String DEFAULT_JETIFIER_VERSION = "1.0.0-beta02";

  /**
   * This is the mandatory dependencies to be excluded from being jetified, as the jetifier rule
   * itself uses those dependencies. Otherwise we'd create dependency cycles
   */
  private static final List<String> JETIFIER_DEPS =
      Arrays.asList(
          "com.android.tools.build.jetifier:jetifier-core",
          "com.android.tools.build.jetifier:jetifier-processor",
          "com.google.code.gson:gson",
          "commons-cli:commons-cli",
          "org.jdom:jdom2",
          "org.jetbrains:annotations",
          "org.ow2.asm:asm-commons",
          "org.ow2.asm:asm-tree",
          "org.ow2.asm:asm-util",
          "org.ow2.asm:asm",
          "org.jetbrains.kotlin:.*");

  /** Jetifier jar version */
  public String version;

  /** Enable jetifier to act on aars only */
  public boolean aarOnly;

  /** Stores the user defined dependencies which are excluded from being jetified. */
  public List<String> exclude = new ArrayList<>();

  /** Path to file containing the custom mapping file to be used on jetifier */
  @Nullable public String customConfigFile;

  private final boolean enableJetifier;

  @Nullable private List<Pattern> excludePatterns;

  JetifierExtension(Project project) {
    version = DEFAULT_JETIFIER_VERSION;
    enableJetifier = JetifierManager.isJetifierEnabled(project);
  }

  private List<Pattern> getExcludePatterns() {
    if (excludePatterns == null) {
      excludePatterns =
          new ImmutableSet.Builder<String>()
              .addAll(exclude)
              .addAll(JETIFIER_DEPS)
              .build()
              .stream()
              .map(Pattern::compile)
              .collect(Collectors.toList());
    }
    return excludePatterns;
  }

  /**
   * Check if this dependency, described by the params, should be jetified, that is, run jetifier on
   * it before prebuilding it.
   *
   * @param group - Dependency group
   * @param name - Dependency name
   * @param packaging - Packaging type (aar\jar)
   * @return true if shouldJetify, false otherwise
   */
  public boolean shouldJetify(String group, String name, String packaging) {
    if (!enableJetifier) {
      return false;
    }
    if (aarOnly && packaging.equals(BaseExternalDependency.JAR)) {
      return false;
    }
    return getExcludePatterns()
        .stream()
        .noneMatch(pattern -> pattern.matcher(group + ":" + name).matches());
  }
}
