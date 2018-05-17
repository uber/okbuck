package com.uber.okbuck.composer.android;

import com.uber.okbuck.core.model.android.AndroidTarget;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.template.android.PrebuiltNativeLibraryRule;
import com.uber.okbuck.template.core.Rule;

public final class PreBuiltNativeLibraryRuleComposer extends AndroidBuckRuleComposer {

  private PreBuiltNativeLibraryRuleComposer() {
    // no instance
  }

  public static Rule compose(AndroidTarget target, String jniLibDir) {
    return new PrebuiltNativeLibraryRule()
        .nativeLibs(jniLibDir)
        .defaultVisibility()
        .ruleType(RuleType.PREBUILT_NATIVE_LIBRARY.getBuckName())
        .name(prebuiltNative(target, jniLibDir));
  }
}
