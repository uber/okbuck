package com.uber.okbuck.composer.android;

import com.uber.okbuck.core.model.android.AndroidAppTarget;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.template.android.InstrumentationApkRule;
import com.uber.okbuck.template.core.Rule;
import java.util.List;
import javax.annotation.Nullable;

public final class AndroidInstrumentationApkRuleComposer extends AndroidBuckRuleComposer {

  private AndroidInstrumentationApkRuleComposer() {
    // no instance
  }

  public static Rule compose(
      List<String> deps, AndroidAppTarget mainApkTarget, @Nullable String manifestRule) {
    return new InstrumentationApkRule()
        .manifest(manifestRule)
        .mainApkRuleName(bin(mainApkTarget))
        .defaultVisibility()
        .ruleType(RuleType.ANDROID_INSTRUMENTATION_APK.getBuckName())
        .name(instrumentation(mainApkTarget))
        .deps(deps);
  }
}
