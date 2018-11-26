package com.uber.okbuck.composer.android;

import com.uber.okbuck.core.model.android.AndroidTarget;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.template.android.ManifestRule;
import com.uber.okbuck.template.core.Rule;

public final class ManifestRuleComposer extends AndroidBuckRuleComposer {

  private ManifestRuleComposer() {
    // no instance
  }

  public static Rule composeForLibrary(AndroidTarget target) {
    return compose(target).pkg(target.getResPackage()).name(libManifest(target));
  }

  public static Rule composeForBinary(AndroidTarget target) {
    return compose(target).pkg(target.getApplicationIdWithSuffix()).name(binManifest(target));
  }

  private static ManifestRule compose(AndroidTarget target) {
    return (ManifestRule)
        new ManifestRule()
            .debuggable(target.getDebuggable())
            .minSdk(target.getMinSdk())
            .targetSdk(target.getTargetSdk())
            .versionCode(target.getVersionCode())
            .versionName(target.getVersionName())
            .mainManifest(target.getMainManifest())
            .secondaryManifests(target.getSecondaryManifests())
            .ruleType(RuleType.MANIFEST.getBuckName());
  }
}
