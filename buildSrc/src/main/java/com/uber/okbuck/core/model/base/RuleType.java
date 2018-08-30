package com.uber.okbuck.core.model.base;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.Var;
import java.util.List;

public enum RuleType {
  ANDROID_BINARY,
  ANDROID_BUILD_CONFIG,
  ANDROID_INSTRUMENTATION_APK,
  ANDROID_INSTRUMENTATION_TEST,
  ANDROID_LIBRARY("java"),
  ANDROID_PREBUILT_AAR("aar"),
  ANDROID_RESOURCE,
  GROOVY_LIBRARY("groovy", "java"),
  GROOVY_TEST("groovy", "java"),
  JAVA_BINARY,
  JAVA_LIBRARY("java"),
  JAVA_TEST("java"),
  KOTLIN_ANDROID_LIBRARY("java", "kt"),
  KOTLIN_LIBRARY("java", "kt"),
  KOTLIN_ROBOLECTRIC_TEST("java", "kt"),
  KOTLIN_TEST("java", "kt"),
  SCALA_LIBRARY("java", "scala"),
  SCALA_TEST("java", "scala"),
  PREBUILT_JAR("binary_jar"),
  PREBUILT_NATIVE_LIBRARY,
  ROBOLECTRIC_TEST("java");

  private final ImmutableList<String> properties;

  RuleType() {
    this("java");
  }

  RuleType(String... properties) {
    this.properties = ImmutableList.copyOf(properties);
  }

  public List<String> getProperties() {
    return properties;
  }

  public String getBuckName() {
    @Var RuleType buckType = this;
    switch (this) {
      case KOTLIN_ANDROID_LIBRARY:
        buckType = ANDROID_LIBRARY;
        break;
      case KOTLIN_ROBOLECTRIC_TEST:
        buckType = ROBOLECTRIC_TEST;
        break;
      default:
        break;
    }
    return buckType.name().toLowerCase();
  }
}
