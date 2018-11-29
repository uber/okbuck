package com.uber.okbuck.extension;

import static com.uber.okbuck.OkBuckGradlePlugin.OKBUCK_PREBUILT_TARGET;
import static com.uber.okbuck.OkBuckGradlePlugin.OKBUCK_TARGETS_TARGET;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.uber.okbuck.core.model.base.RuleType;
import groovy.lang.Closure;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.gradle.api.Project;

/*
Note: Do not merge this section with javadoc to prevent GJF from butchering it.
Example:

ruleOverrides {
    defaultImportLocation = "//tooling/buck-defs:my_targets.bzl"
    defaultRuleNamePrefix = "my_company_"
    override {
        nativeRuleName = "java_library"
    }
    override {
        nativeRuleName = "java_test"
    }
    override {
        nativeRuleName = "java_binary"
    }
    override {
        nativeRuleName = "scala_library"
        importLocation = "//tooling/buck-defs:experimental.bzl"
        newRuleName = "experimental_scala_library"
    }
    override {
        nativeRuleName = "scala_test"
        importLocation = "//tooling/buck-defs:experimental.bzl"
        newRuleName = "experimental_scala_test"
    }
}
*/
/**
 * Custom overrides for rules re-defined within project See example above.
 *
 * <p>Note, for some of the rules, okBuck may generate own rule types, for example
 * "okbuck_android_library". To replace it with own override, please use "android_library" as
 * nativeRuleName
 */
public class RuleOverridesExtension {

  // okBuck pre-defined rules
  private static final String OKBUCK_PREFIX = "okbuck_";
  private static final ImmutableMap<RuleType, String> OKBUCK_DEFINED_RULES =
      ImmutableMap.<RuleType, String>builder()
          .put(RuleType.AIDL, OKBUCK_TARGETS_TARGET)
          .put(RuleType.PREBUILT, OKBUCK_PREBUILT_TARGET)
          .put(RuleType.PREBUILT_JAR, OKBUCK_TARGETS_TARGET)
          .put(RuleType.ANDROID_LIBRARY, OKBUCK_TARGETS_TARGET)
          .put(RuleType.ANDROID_PREBUILT_AAR, OKBUCK_TARGETS_TARGET)
          .put(RuleType.KOTLIN_ANDROID_LIBRARY, OKBUCK_TARGETS_TARGET)
          .put(RuleType.KEYSTORE, OKBUCK_TARGETS_TARGET)
          .put(RuleType.MANIFEST, OKBUCK_TARGETS_TARGET)
          .build();

  private Map<String, OverrideSetting> overridesMap = Collections.emptyMap();
  private final List<RawOverrideSetting> overrides = new ArrayList<>();

  /** Import location to be used by default for overrides. */
  @Nullable private String defaultImportLocation;

  /** Rule name prefix to be used by default for overrides. */
  @Nullable private String defaultRuleNamePrefix;

  /** Override section for the rule. */
  protected void override(Closure<RawOverrideSetting> action) {
    RawOverrideSetting setting = new RawOverrideSetting();
    action.setDelegate(setting);
    action.call(setting);
    overrides.add(setting);
  }

  public Map<String, OverrideSetting> getOverrides() {
    return overridesMap;
  }

  RuleOverridesExtension(Project project) {
    project.afterEvaluate(
        evaluatedProject -> {
          validateExtension();
          preProcessExtension();
        });
  }

  private void preProcessExtension() {
    Map<String, OverrideSetting> configuredOverrides =
        overrides
            .stream()
            .map(this::applyDefaults)
            .collect(Collectors.toMap(RawOverrideSetting::getNativeRuleName, OverrideSetting::new));

    // Add OkBuck defaults for rules not re-defined by user.
    OKBUCK_DEFINED_RULES.forEach(
        ((ruleType, target) -> {
          String buckName = ruleType.getBuckName();
          configuredOverrides.computeIfAbsent(
              buckName,
              nativeRuleName -> new OverrideSetting(target, OKBUCK_PREFIX + nativeRuleName));
        }));

    overridesMap = ImmutableMap.copyOf(configuredOverrides);
  }

  private RawOverrideSetting applyDefaults(RawOverrideSetting originalSetting) {
    return new RawOverrideSetting(
        originalSetting.getNativeRuleName(),
        Optional.ofNullable(Strings.emptyToNull(originalSetting.getImportLocation()))
            .orElse(defaultImportLocation),
        Optional.ofNullable(Strings.emptyToNull(originalSetting.getNewRuleName()))
            .orElse(defaultRuleNamePrefix + originalSetting.getNativeRuleName()));
  }

  private void validateExtension() {
    Set<String> usedOverrideKeys = new HashSet<>();
    for (RawOverrideSetting baseSetting : overrides) {
      RawOverrideSetting setting = applyDefaults(baseSetting);
      Preconditions.checkNotNull(
          Strings.emptyToNull(setting.getNativeRuleName()),
          "nativeRuleName is required for override");
      Preconditions.checkNotNull(
          Strings.emptyToNull(setting.getImportLocation()),
          setting.getNativeRuleName()
              + ": importLocation is required for override,"
              + " since no defaultImportLocation is specified");
      Preconditions.checkNotNull(
          Strings.emptyToNull(setting.getNewRuleName()),
          setting.getNativeRuleName()
              + ": newRuleName is required for override,"
              + " since no defaultRuleNamePrefix is specified");

      if (usedOverrideKeys.contains(setting.getNativeRuleName())) {
        throw new IllegalArgumentException(
            "Multiple overrides specified for the same rule: " + setting.getNativeRuleName());
      } else {
        usedOverrideKeys.add(setting.getNativeRuleName());
      }
    }
  }

  public static class OverrideSetting {
    private String importLocation;
    private String newRuleName;

    private OverrideSetting(RawOverrideSetting rawOverrideSetting) {
      // At this point rawOverrideSetting should be validated.
      this(
          Preconditions.checkNotNull(rawOverrideSetting.getImportLocation()),
          Preconditions.checkNotNull(rawOverrideSetting.getNewRuleName()));
    }

    private OverrideSetting(String importLocation, String newRuleName) {
      this.importLocation = importLocation;
      this.newRuleName = newRuleName;
    }

    public String getImportLocation() {
      return importLocation;
    }

    public String getNewRuleName() {
      return newRuleName;
    }
  }

  private static class RawOverrideSetting {
    @Nullable private String nativeRuleName;
    @Nullable private String importLocation;
    @Nullable private String newRuleName;

    public RawOverrideSetting() {}

    private RawOverrideSetting(
        @Nullable String nativeRuleName,
        @Nullable String importLocation,
        @Nullable String newRuleName) {
      this.nativeRuleName = nativeRuleName;
      this.importLocation = importLocation;
      this.newRuleName = newRuleName;
    }

    @Nullable
    public String getNativeRuleName() {
      return nativeRuleName;
    }

    @Nullable
    public String getImportLocation() {
      return importLocation;
    }

    @Nullable
    public String getNewRuleName() {
      return newRuleName;
    }
  }
}
