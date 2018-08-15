package com.uber.okbuck.composer.android;

import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.manager.LintManager;
import com.uber.okbuck.core.model.android.AndroidTarget;
import com.uber.okbuck.core.model.base.Target;
import com.uber.okbuck.core.model.jvm.JvmTarget;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.LintExtension;
import com.uber.okbuck.template.android.LintRule;
import com.uber.okbuck.template.core.Rule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class LintRuleComposer extends AndroidBuckRuleComposer {

  private LintRuleComposer() {
    // no instance
  }

  public static Rule compose(AndroidTarget target) {
    String lintConfigXml = "";
    if (target.getLintOptions() != null
        && target.getLintOptions().getLintConfig() != null
        && target.getLintOptions().getLintConfig().exists()) {
      lintConfigXml =
          ProjectUtil.getLintConfigRule(
              target.getProject(), target.getLintOptions().getLintConfig());
    }

    Set<Target> customLintTargets =
        target
            .getLint()
            .getTargetDeps()
            .stream()
            .filter(t -> (t instanceof JvmTarget) && ((JvmTarget) t).hasLintRegistry())
            .collect(Collectors.toSet());

    final List<String> customLintRules =
        new ArrayList<>(BuckRuleComposer.external(target.getMain().getPackagedLintJars()));

    customLintTargets
        .stream()
        .filter(it -> it instanceof JvmTarget && ((JvmTarget) it).hasApplication())
        .forEach(it -> customLintRules.add(BuckRuleComposer.binTargets(it)));

    LintExtension lintExtension = target.getOkbuck().getLintExtension();
    return new LintRule()
        .manifest(fileRule(target.getManifest()))
        .sources(target.getMain().getSources())
        .resources(target.getResDirs())
        .customLints(customLintRules)
        .jvmArgs(lintExtension.jvmArgs)
        .deps(Collections.singletonList(LintManager.LINT_DEPS_RULE))
        .lintConfigXml(lintConfigXml)
        .lintOptions(target.getLintOptions())
        .name("lint_" + target.getName());
  }
}
