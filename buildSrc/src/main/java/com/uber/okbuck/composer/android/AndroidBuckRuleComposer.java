package com.uber.okbuck.composer.android;

import com.uber.okbuck.composer.jvm.JvmBuckRuleComposer;
import com.uber.okbuck.core.model.android.AndroidAppTarget;
import com.uber.okbuck.core.model.android.AndroidTarget;
import com.uber.okbuck.core.model.base.Target;
import java.util.Set;
import java.util.stream.Collectors;

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

  public static String libManifest(AndroidTarget target) {
    return "manifest_lib_" + target.getName();
  }

  static String binManifest(AndroidTarget target) {
    return "manifest_bin_" + target.getName();
  }

  public static String keystore(AndroidTarget target) {
    return "keystore_" + target.getName();
  }

  static String appLib(AndroidTarget target) {
    return "app_lib_" + target.getName();
  }

  public static String bin(AndroidAppTarget target) {
    return "bin_" + target.getName();
  }

  public static String instrumentation(AndroidAppTarget target) {
    return "instrumentation_" + target.getName() + "_apk";
  }

  static String instrumentationTest(AndroidAppTarget target) {
    return "instrumentation_" + target.getName() + "_test";
  }

  static Set<String> resources(Set<Target> targets) {
    return targets
        .stream()
        .filter(targetDep -> targetDep instanceof AndroidTarget)
        .map(targetDep -> resRule((AndroidTarget) targetDep))
        .collect(Collectors.toSet());
  }
}
