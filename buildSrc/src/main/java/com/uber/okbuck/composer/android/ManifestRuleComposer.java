package com.uber.okbuck.composer.android;

import com.uber.okbuck.core.model.android.AndroidTarget;
import com.uber.okbuck.template.android.ManifestRule;
import com.uber.okbuck.template.core.Rule;

public final class ManifestRuleComposer extends AndroidBuckRuleComposer {

  private ManifestRuleComposer() {
    // no instance
  }

  public static Rule compose(AndroidTarget target) {
    return new ManifestRule()
        .debuggable(target.getDebuggable())
        .minSdk(target.getMinSdk())
        .targetSdk(target.getTargetSdk())
        .versionCode(target.getVersionCode())
        .versionName(target.getVersionName())
        .applicationPackage(target.getApplicationPackage())
        .mainManifest(target.getMainManifest())
        .secondaryManifests(target.getSecondaryManifests())
        .name(manifest(target));
  }
}
