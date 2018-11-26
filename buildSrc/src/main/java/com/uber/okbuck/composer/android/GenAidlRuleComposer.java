package com.uber.okbuck.composer.android;

import com.uber.okbuck.core.model.android.AndroidTarget;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.template.android.GenAidlRule;
import com.uber.okbuck.template.core.Rule;

public final class GenAidlRuleComposer extends AndroidBuckRuleComposer {

  private GenAidlRuleComposer() {
    // no instance
  }

  public static Rule compose(AndroidTarget target, String aidlDir, String manifestRule) {
    return new GenAidlRule()
        .aidlFilePath(aidlDir)
        .importPath(target.getPath() + "/" + aidlDir)
        .manifest(manifestRule)
        .aidlDeps(targets(target.getMain().getTargetDeps()))
        .name(aidl(target, aidlDir))
        .ruleType(RuleType.AIDL.getBuckName());
  }
}
