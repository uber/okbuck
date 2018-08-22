package com.uber.okbuck.composer.android;

import com.uber.okbuck.core.model.android.AndroidTarget;
import com.uber.okbuck.template.android.ManifestRule;
import com.uber.okbuck.template.core.Rule;

public final class ManifestRuleComposer extends AndroidBuckRuleComposer {

  private ManifestRuleComposer() {
    //no instance
  }

  public static Rule compose(final AndroidTarget target) {
    return new ManifestRule()
        .debuggable(target.getDebuggable())
        .minSdk(target.getMinSdk())
        .targetSdk(target.getTargetSdk())
        .versionCode(target.getVersionCode())
        .versionName(target.getVersionName())
        .mainManifest(target.getMainManifest())
        .name(manifest(target));
  }

}
