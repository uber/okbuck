package com.uber.okbuck.composer.android;

import com.uber.okbuck.core.model.android.AndroidTarget;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.template.android.ResourceRule;
import com.uber.okbuck.template.core.Rule;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public final class AndroidResourceRuleComposer extends AndroidBuckRuleComposer {

  private AndroidResourceRuleComposer() {
    // no instance
  }

  public static Rule compose(AndroidTarget target, List<String> extraResDeps) {
    List<String> resDeps = new ArrayList<>();
    resDeps.addAll(external(new HashSet<>(target.getMain().getExternalAarDeps())));
    resDeps.addAll(
        target
            .getTargetDeps(false)
            .stream()
            .filter(targetDep -> targetDep instanceof AndroidTarget)
            .map(targetDep -> resRule((AndroidTarget) targetDep))
            .collect(Collectors.toSet()));

    resDeps.addAll(extraResDeps);

    return new ResourceRule()
        .pkg(target.getResPackage())
        .res(target.getResDirs())
        .projectRes(target.getProjectResDir())
        .assets(target.getAssetDirs())
        .resourceUnion(target.getOkbuck().useResourceUnion())
        .defaultVisibility()
        .ruleType(RuleType.ANDROID_RESOURCE.getBuckName())
        .deps(resDeps)
        .name(res(target));
  }
}
