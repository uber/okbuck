package com.uber.okbuck.composer.android;

import com.uber.okbuck.core.model.android.AndroidTarget;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.template.android.ResourceRule;
import com.uber.okbuck.template.core.Rule;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AndroidResourceRuleComposer extends AndroidBuckRuleComposer {

  private AndroidResourceRuleComposer() {
    // no instance
  }

  public static Rule compose(AndroidTarget target, List<String> extraResDeps) {
    Set<String> resDeps = new HashSet<>();
    resDeps.addAll(external(target.getExternalAarDeps(false)));
    resDeps.addAll(resources(target.getTargetDeps(false)));
    resDeps.addAll(extraResDeps);

    Set<String> resExportedDeps = new HashSet<>();
    resExportedDeps.addAll(external(target.getExternalExportedAarDeps(false)));
    resExportedDeps.addAll(resources(target.getTargetExportedDeps(false)));

    return new ResourceRule()
        .pkg(target.getResPackage())
        .res(target.getResDirs())
        .projectRes(target.getProjectResDir())
        .assets(target.getAssetDirs())
        .resourceUnion(target.getOkbuck().useResourceUnion())
        .exportedDeps(resExportedDeps)
        .defaultVisibility()
        .ruleType(RuleType.ANDROID_RESOURCE.getBuckName())
        .deps(resDeps)
        .name(res(target));
  }
}
