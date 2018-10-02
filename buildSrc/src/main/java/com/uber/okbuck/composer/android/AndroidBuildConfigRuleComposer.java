package com.uber.okbuck.composer.android;

import com.uber.okbuck.core.model.android.AndroidTarget;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.template.android.BuildConfigRule;
import com.uber.okbuck.template.core.Rule;

public final class AndroidBuildConfigRuleComposer extends AndroidBuckRuleComposer {

  private AndroidBuildConfigRuleComposer() {
    // no instance
  }

  public static Rule compose(AndroidTarget target) {
    return new BuildConfigRule()
        .pkg(target.getPackage())
        .values(target.getBuildConfigFields())
        .defaultVisibility()
        .ruleType(RuleType.ANDROID_BUILD_CONFIG.getBuckName())
        .name(AndroidBuckRuleComposer.buildConfig(target));
  }
}
