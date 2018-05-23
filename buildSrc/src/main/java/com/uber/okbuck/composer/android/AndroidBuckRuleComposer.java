package com.uber.okbuck.composer.android;

import com.uber.okbuck.composer.jvm.JvmBuckRuleComposer;
import com.uber.okbuck.core.model.android.AndroidAppTarget;
import com.uber.okbuck.core.model.android.AndroidLibInstrumentationTarget;
import com.uber.okbuck.core.model.android.AndroidTarget;

public abstract class AndroidBuckRuleComposer extends JvmBuckRuleComposer {

  public static String res(AndroidTarget target) {
    return "res_" + target.getName();
  }

  static String resRule(AndroidTarget target) {
    return "//" + target.getPath() + ":" + res(target);
  }

  static String buildConfig(AndroidTarget target) {
    return "build_config_" + target.getName();
  }

  static String prebuiltNative(AndroidTarget target, String jniLibDir) {
    return "prebuilt_native_library_" + target.getName() + "_" + jniLibDir.replaceAll("/", "_");
  }

  public static String aidl(AndroidTarget target, String aidlDir) {
    return target.getName() + "_" + aidlDir.replaceAll("[/-]", "_") + "_aidls";
  }

  static String appLib(AndroidTarget target) {
    return "app_lib_" + target.getName();
  }

  public static String bin(AndroidAppTarget target) {
    return "bin_" + target.getName();
  }

  /**
   * This method is intentionally called bin because libraries test apks are actually android_binary
   * rules. So this overrides the behavior of the {@link
   * AndroidBuckRuleComposer#bin(AndroidAppTarget)} the get the right name.
   */
  public static String bin(AndroidLibInstrumentationTarget target) {
    return "lib_instrumentation_" + target.getName() + "_apk";
  }

  public static String instrumentation(AndroidAppTarget target) {
    return "instrumentation_" + target.getName() + "_apk";
  }

  static String instrumentationTest(AndroidAppTarget target) {
    return "instrumentation_" + target.getName() + "_test";
  }
}
