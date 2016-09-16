package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.AndroidAppTarget
import com.github.okbuilds.core.model.AndroidTarget

abstract class AndroidBuckRuleComposer extends JavaBuckRuleComposer {

    static String res(AndroidTarget target, AndroidTarget.ResBundle bundle) {
        return "//${target.path}:${resLocal(bundle)}"
    }

    static String resLocal(AndroidTarget.ResBundle bundle) {
        return "res_${bundle.name}"
    }

    static String manifest(AndroidTarget target) {
        return "manifest_${target.name}"
    }

    static String buildConfig(AndroidTarget target) {
        return "build_config_${target.name}"
    }

    static String prebuiltNative(AndroidTarget target, String jniLibDir) {
        return "prebuilt_native_library_${target.name}_${jniLibDir.replaceAll("/", "_")}"
    }

    static String aidl(AndroidTarget target) {
        return "${target.name}_aidls"
    }

    static String appLib(AndroidTarget target) {
        return "app_lib_${target.name}"
    }

    static String bin(AndroidAppTarget target) {
        return "bin_${target.name}"
    }

    static String instrumentation(AndroidAppTarget target) {
        return "instrumentation_${target.name}_apk"
    }

    static String instrumentationTest(AndroidAppTarget target) {
        return "instrumentation_${target.name}_test"
    }
}
