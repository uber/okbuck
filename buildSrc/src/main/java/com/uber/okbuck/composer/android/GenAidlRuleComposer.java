package com.uber.okbuck.composer.android;

import com.uber.okbuck.core.model.android.AndroidTarget;
import com.uber.okbuck.template.android.GenAidlRule;
import com.uber.okbuck.template.core.Rule;

public final class GenAidlRuleComposer extends AndroidBuckRuleComposer {

  private GenAidlRuleComposer() {
    // no instance
  }

  public static Rule compose(final AndroidTarget target, final String aidlDir) {
    return new GenAidlRule()
        .aidlFilePath(aidlDir)
        .importPath(target.getPath() + "/" + aidlDir)
        .manifest(fileRule(target.getManifest()))
        .aidlDeps(targets(target.getMain().getTargetDeps()))
        .name(aidl(target, aidlDir));
  }
}
