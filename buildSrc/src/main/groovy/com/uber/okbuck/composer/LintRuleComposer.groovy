package com.uber.okbuck.composer

import com.uber.okbuck.core.model.JavaAppTarget
import com.uber.okbuck.core.model.JavaTarget
import com.uber.okbuck.core.model.Target
import com.uber.okbuck.core.util.LintUtil
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

        Set<String> customLintTargets = [] as Set
        Set<Target> lintJarTargets = target.lint.targetDeps.findAll {
            (it instanceof JavaTarget) && (it.hasLintRegistry())
        }

        customLintTargets.addAll(targets(lintJarTargets))
        customLintTargets.addAll(external(target.main.packagedLintJars))

        Set<String> classpathLintDeps = [] as Set
        Set<String> locationLintDeps = [] as Set

        locationLintDeps.addAll(LintUtil.LINT_DEPS_RULE)
        lintJarTargets.each {
            if (it instanceof JavaAppTarget) {
                locationLintDeps.addAll(targets([it] as Set))
            } else {
                classpathLintDeps.addAll(targets([it] as Set))
            }
        }

        return new LintRule(
                lint(target),
                inputs,
                LintUtil.getLintwRule(target),
                customLintTargets,
                classpathLintDeps,
                locationLintDeps,
                target.main.sources.empty ? null : ":${src(target)}")
    }
}
