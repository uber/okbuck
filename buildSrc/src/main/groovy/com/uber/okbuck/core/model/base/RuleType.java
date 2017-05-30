package com.uber.okbuck.core.model.base;

public enum RuleType {
    ANDROID_BINARY,
    ANDROID_BUILD_CONFIG,
    ANDROID_INSTRUMENTATION_APK,
    ANDROID_INSTRUMENTATION_TEST,
    ANDROID_LIBRARY,
    ANDROID_MANIFEST,
    ANDROID_RESOURCE,
    GEN_AIDL,
    GENRULE,
    GROOVY_LIBRARY,
    GROOVY_TEST,
    JAVA_BINARY,
    JAVA_LIBRARY,
    JAVA_TEST,
    KOTLIN_ANDROID_LIBRARY("kt"),
    KOTLIN_LIBRARY("kt"),
    KOTLIN_TEST("kt"),
    PREBUILT_NATIVE_LIBRARY,
    ROBOLECTRIC_TEST;

    private final String sourceExtension;

    RuleType() {
        this("java");
    }

    RuleType(String sourceExtension) {
        this.sourceExtension = sourceExtension;
    }

    public String getSourceExtension() {
        return sourceExtension;
    }
}
