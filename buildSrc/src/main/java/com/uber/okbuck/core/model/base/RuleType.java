package com.uber.okbuck.core.model.base;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Var;

public enum RuleType {
  AIDL,
  ANDROID_BINARY,
  ANDROID_BUILD_CONFIG,
  ANDROID_INSTRUMENTATION_APK,
  ANDROID_INSTRUMENTATION_TEST,
  ANDROID_LIBRARY("java"),
  ANDROID_MODULE("java"),
  ANDROID_PREBUILT_AAR("aar"),
  ANDROID_RESOURCE,
  GROOVY_LIBRARY("groovy", "java"),
  GROOVY_TEST("groovy", "java"),
  GROOVY_INTEGRATION_TEST("groovy", "java"),
  JAVA_ANNOTATION_PROCESSOR,
  JAVA_BINARY,
  JAVA_LIBRARY("java"),
  JAVA_TEST("java"),
  JAVA_INTEGRATION_TEST("java"),
  KEYSTORE,
  KOTLIN_ANDROID_LIBRARY("java", "kt"),
  KOTLIN_ANDROID_MODULE("java", "kt"),
  KOTLIN_LIBRARY("java", "kt"),
  KOTLIN_ROBOLECTRIC_TEST("java", "kt"),
  KOTLIN_TEST("java", "kt"),
  KOTLIN_INTEGRATION_TEST("java", "kt"),
  MANIFEST,
  SCALA_LIBRARY("java", "scala"),
  SCALA_TEST("java", "scala"),
  SCALA_INTEGRATION_TEST("java", "scala"),
  PREBUILT_JAR("binary_jar"),
  PREBUILT,
  PREBUILT_NATIVE_LIBRARY,
  ROBOLECTRIC_TEST("java");

  private final ImmutableList<String> properties;

  RuleType() {
    this("java");
  }

  RuleType(String... properties) {
    this.properties = ImmutableList.copyOf(properties);
  }

  public ImmutableList<String> getProperties() {
    return properties;
  }

  public String getBuckName() {
    @Var RuleType ruleType = this;
    switch (this) {
      case KOTLIN_ANDROID_LIBRARY:
      case ANDROID_LIBRARY:
        ruleType = ANDROID_LIBRARY;
        break;
      case KOTLIN_ROBOLECTRIC_TEST:
      case ROBOLECTRIC_TEST:
        ruleType = ROBOLECTRIC_TEST;
        break;
      default:
        break;
    }

    return ruleType.name().toLowerCase();
  }
}
