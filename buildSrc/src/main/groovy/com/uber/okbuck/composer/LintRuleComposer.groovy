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

        Set<String> customLintTargets = [] as Set
        Set<Target> lintJarTargets = target.lint.targetDeps.findAll {
            (it instanceof JavaTarget) && (it.hasLintRegistry())
        }

        customLintTargets.addAll(external(target.main.packagedLintJars))
        customLintTargets.addAll(targets(lintJarTargets))

        Set<String> classpathLintDeps = [] as Set
        Set<String> locationLintDeps = [] as Set

        locationLintDeps.addAll(LintUtil.LINT_DEPS_RULE)
        lintJarTargets.each {
            if (ProjectUtil.getType(it.project) == ProjectType.JAVA_APP) {
                locationLintDeps.addAll(binTargets(it))
            } else {
                classpathLintDeps.addAll(targets(it))
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
