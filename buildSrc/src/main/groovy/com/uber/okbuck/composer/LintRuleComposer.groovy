package com.uber.okbuck.composer

import com.uber.okbuck.core.model.JavaTarget
import com.uber.okbuck.core.model.ProjectType
import com.uber.okbuck.core.model.Target
import com.uber.okbuck.core.util.LintUtil
import com.uber.okbuck.core.util.ProjectUtil
import com.uber.okbuck.extension.LintExtension
import com.uber.okbuck.generator.LintWrapperGenerator
import com.uber.okbuck.rule.LintRule

final class LintRuleComposer extends JavaBuckRuleComposer {

    private LintRuleComposer() {
        // no instance
    }

    static LintRule compose(JavaTarget target) {
        Set<String> inputs = []
        if (target.lintOptions.lintConfig != null && target.lintOptions.lintConfig.exists()) {
            inputs.add(LintUtil.getLintwConfigRule(target.project, target.lintOptions.lintConfig))
        }

        LintExtension lintExtension = target.rootProject.okbuck.lint
        LintWrapperGenerator.generate(target, lintExtension.jvmArgs)

        Set<Target> customLintTargets = target.lint.targetDeps.findAll {
            (it instanceof JavaTarget) && (it.hasLintRegistry())
        }

        Set<String> customLintRules = [] as Set
        customLintRules.addAll(external(target.main.packagedLintJars))
        customLintRules.addAll(targets(customLintTargets))

        Set<String> lintDeps = [] as Set
        lintDeps.addAll(LintUtil.LINT_DEPS_RULE)
        customLintTargets.each {
            if (ProjectUtil.getType(it.project) == ProjectType.JAVA_APP) {
                lintDeps.addAll(binTargets(it))
            }
        }

        return new LintRule(
                lint(target),
                inputs,
                LintUtil.getLintwRule(target),
                customLintRules,
                lintDeps,
                target.main.sources.empty ? null : ":${src(target)}")
    }
}
