package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidTarget
import com.uber.okbuck.core.model.base.Target
import com.uber.okbuck.core.model.java.JavaLibTarget
import com.uber.okbuck.core.model.java.JavaTarget
import com.uber.okbuck.core.util.LintUtil
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
            lintConfigXml = LintUtil.getLintwConfigRule(target.project, target.lintOptions.lintConfig)
        }

        Set<Target> customLintTargets = target.lint.targetDeps.findAll {
            (it instanceof JavaTarget) && (it.hasLintRegistry())
        }

        List<String> customLintRules = []
        customLintRules.addAll(external(target.main.packagedLintJars))
        customLintTargets.each {
            if (it instanceof JavaLibTarget && it.hasApplication()) {
                customLintRules.add(binTargets(it))
            }
        }

        List<String> lintDeps = []
        lintDeps.addAll(LintUtil.LINT_DEPS_RULE)

        LintExtension lintExtension = target.rootProject.okbuck.lint
        return new LintRule()
                .manifest(manifest(target))
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
