package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidTarget
import com.uber.okbuck.core.model.base.Target
import com.uber.okbuck.core.model.jvm.JvmTarget
import com.uber.okbuck.core.util.LintUtil
import com.uber.okbuck.core.util.ProjectUtil
import com.uber.okbuck.extension.LintExtension
import com.uber.okbuck.template.android.LintRule
import com.uber.okbuck.template.core.Rule

final class LintRuleComposer extends AndroidBuckRuleComposer {

    private LintRuleComposer() {
        // no instance
    }

    static Rule compose(AndroidTarget target) {
        String lintConfigXml = ""
        if (target.lintOptions.lintConfig != null && target.lintOptions.lintConfig.exists()) {
            lintConfigXml = ProjectUtil.getLintwConfigRule(target.project, target.lintOptions.lintConfig)
        }

        Set<Target> customLintTargets = target.lint.targetDeps.findAll {
            (it instanceof JvmTarget) && (it.hasLintRegistry())
        }

        List<String> customLintRules = []
        customLintRules.addAll(external(target.main.packagedLintJars))
        customLintTargets.each {
            if (it instanceof JvmTarget && it.hasApplication()) {
                customLintRules.add(binTargets(it))
            }
        }

        List<String> lintDeps = []
        lintDeps.addAll(LintUtil.LINT_DEPS_RULE)

        LintExtension lintExtension = target.getOkbuck().getLintExtension()
        return new LintRule()
                .manifest(fileRule(target.manifest))
                .sources(target.main.sources)
                .resources(target.resDirs)
                .customLints(customLintRules)
                .jvmArgs(lintExtension.jvmArgs)
                .deps(lintDeps)
                .lintConfigXml(lintConfigXml)
                .lintOptions(target.lintOptions)
                .name("lint_${target.name}")
    }
}
