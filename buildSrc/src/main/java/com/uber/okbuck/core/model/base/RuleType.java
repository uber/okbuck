package com.uber.okbuck.core.model.base;

import com.google.common.collect.ImmutableList;
import java.util.List;

public enum RuleType {
  ANDROID_BINARY,
  ANDROID_BUILD_CONFIG,
  ANDROID_INSTRUMENTATION_APK,
  ANDROID_INSTRUMENTATION_TEST,
  ANDROID_LIBRARY,
  ANDROID_RESOURCE,
  GROOVY_LIBRARY("groovy", "java"),
  GROOVY_TEST("groovy", "java"),
  JAVA_BINARY,
  JAVA_LIBRARY,
  JAVA_TEST,
  KOTLIN_ANDROID_LIBRARY("java", "kt"),
  KOTLIN_LIBRARY("java", "kt"),
  KOTLIN_ROBOLECTRIC_TEST("java", "kt"),
  KOTLIN_TEST("java", "kt"),
  SCALA_LIBRARY("java", "scala"),
  SCALA_TEST("java", "scala"),
  PREBUILT_NATIVE_LIBRARY,
  ROBOLECTRIC_TEST;

  private final ImmutableList<String> sourceExtensions;

  RuleType() {
    this("java");
  }

  RuleType(String... extensions) {
    this.sourceExtensions = ImmutableList.copyOf(extensions);
  }

  public List<String> getSourceExtensions() {
    return sourceExtensions;
  }

  public String getBuckName() {
    RuleType buckType = this;
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
