package com.uber.okbuck.composer

import com.uber.okbuck.core.model.AndroidTarget
import com.uber.okbuck.core.model.JavaTarget
import com.uber.okbuck.core.model.ProjectType
import com.uber.okbuck.core.model.Target
import com.uber.okbuck.core.util.LintUtil
import com.uber.okbuck.core.util.ProjectUtil
import com.uber.okbuck.extension.LintExtension
import com.uber.okbuck.rule.GenRule

final class LintRuleComposer extends JavaBuckRuleComposer {

    static final String SEPARATOR = ':'

    private LintRuleComposer() {
        // no instance
    }

    static GenRule compose(JavaTarget target) {
        List<String> inputs = []
        if (target.lintOptions.lintConfig != null && target.lintOptions.lintConfig.exists()) {
            inputs.add(LintUtil.getLintwConfigRule(target.project, target.lintOptions.lintConfig))
        }

        List<Target> customLintTargets = target.lint.targetDeps.findAll {
            (it instanceof JavaTarget) && (it.hasLintRegistry())
        } as List

        List<String> customLintRules = []
        customLintRules.addAll(external(target.main.packagedLintJars))
        customLintRules.addAll(targets(customLintTargets as Set))

        List<String> lintDeps = []
        lintDeps.addAll(LintUtil.LINT_DEPS_RULE)
        customLintTargets.each {
            if (ProjectUtil.getType(it.project) == ProjectType.JAVA_APP) {
                lintDeps.add(binTargets(it))
            }
        }

        List<String> lintCmds = []
        if (customLintRules) {
            lintCmds.add("export ANDROID_LINT_JARS=\"${toLocation(customLintRules)}\";")
        }
        lintCmds += ["mkdir -p \$OUT;", "exec java", "-Djava.awt.headless=true"]

        LintExtension lintExtension = target.rootProject.okbuck.lint
        if (lintExtension.jvmArgs) {
            lintCmds.add(lintExtension.jvmArgs)
        }
        if (lintDeps) {
            lintCmds.add("-classpath ${toLocation(lintDeps)}")
        }
        lintCmds.add("com.android.tools.lint.Main")

        if (!target.main.sources.empty) {
            lintCmds.add("--classpath ${toLocation(":${src(target)}")}")
        }
        if (target.lintOptions.abortOnError) {
            lintCmds.add("--exitcode")
        }
        if (target.lintOptions.absolutePaths) {
            lintCmds.add("--fullpath")
        }
        if (target.lintOptions.quiet) {
            lintCmds.add("--quiet")
        }
        if (target.lintOptions.checkAllWarnings) {
            lintCmds.add("-Wall")
        }
        if (target.lintOptions.ignoreWarnings) {
            lintCmds.add("--nowarn")
        }
        if (target.lintOptions.warningsAsErrors) {
            lintCmds.add("-Werror")
        }
        if (target.lintOptions.noLines) {
            lintCmds.add("--nolines")
        }
        if (target.lintOptions.disable) {
            lintCmds.add("--disable ${target.lintOptions.disable.join(',')}")
        }
        if (target.lintOptions.enable) {
            lintCmds.add("--enable ${target.lintOptions.enable.join(',')}")
        }
        if (target.lintOptions.lintConfig && target.lintOptions.lintConfig.exists()) {
            lintCmds.add("--config ${target.lintOptions.lintConfig.absolutePath}")
        }
        if (target.lintOptions.xmlReport) {
            lintCmds.add('--xml "\$OUT/lint-results.xml"')
        }
        if (target.lintOptions.htmlReport) {
            lintCmds.add('--html "\$OUT/lint-results.html"')
        }
        target.main.sources.each { String sourceDir ->
            lintCmds.add("--sources ${new File(target.project.projectDir, sourceDir).absolutePath}")
        }
        if (target instanceof AndroidTarget) {
            target.getResources().each { AndroidTarget.ResBundle bundle ->
                if (bundle.resDir) {
                    lintCmds.add("--resources ${new File(target.project.projectDir, bundle.resDir).absolutePath}")
                }
            }

            // Project root is at okbuck generated manifest for this target
            lintCmds.add("${new File(target.project.projectDir, target.manifest).parentFile.absolutePath}")
        }

        return new GenRule(
                lint(target),
                inputs,
                lintCmds)
    }

    static String toLocation(List<String> targets) {
        return (targets.collect { toLocation(it) }).join(SEPARATOR)
    }

    static String toLocation(String target) {
        return "\$(location ${target})"
    }
}
