package com.uber.okbuck.core.model.base;

import java.util.Arrays;
import java.util.List;

public enum RuleType {
    ANDROID_BINARY,
    ANDROID_BUILD_CONFIG,
    ANDROID_INSTRUMENTATION_APK,
    ANDROID_INSTRUMENTATION_TEST,
    ANDROID_LIBRARY,
    ANDROID_LIBRARY_WITH_KOTLIN("java", "kt"),
    ANDROID_MANIFEST,
    ANDROID_RESOURCE,
    GEN_AIDL,
    GENRULE,
    GROOVY_LIBRARY("groovy", "java"),
    GROOVY_TEST("groovy", "java"),
    JAVA_BINARY,
    JAVA_LIBRARY,
    JAVA_TEST,
    KOTLIN_LIBRARY("java", "kt"),
    KOTLIN_TEST("java", "kt"),
    PREBUILT_NATIVE_LIBRARY,
    ROBOLECTRIC_TEST,
    ROBOLECTRIC_TEST_WITH_KOTLIN("java", "kt");

    private final List<String> sourceExtensions;

    RuleType() {
        this("java");
    }

    RuleType(String... extensions) {
        this.sourceExtensions = Arrays.asList(extensions);
    }

    public List<String> getSourceExtensions() {
        return sourceExtensions;
    }
}
