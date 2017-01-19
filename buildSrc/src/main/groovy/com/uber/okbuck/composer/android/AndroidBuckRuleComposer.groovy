package com.uber.okbuck.composer.android

import com.uber.okbuck.composer.java.JavaBuckRuleComposer
import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.core.model.android.AndroidTarget

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

    static String aidl(AndroidTarget target, String aidlDir) {
        return "${target.name}_${aidlDir}_aidls"
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

    static String transform(String runnerClass, AndroidAppTarget target) {
        return "transform_${runnerClass.replace(".", "_")}_${target.name}"
    }
}
