package com.uber.okbuck.composer.android

import com.uber.okbuck.composer.jvm.JvmBuckRuleComposer
import com.uber.okbuck.core.model.android.AndroidTarget
import com.uber.okbuck.core.model.base.ProjectType
import com.uber.okbuck.core.model.base.Target
import com.uber.okbuck.core.model.java.JavaTarget
import com.uber.okbuck.core.util.LintUtil
import com.uber.okbuck.core.util.ProjectUtil
import com.uber.okbuck.extension.LintExtension
import com.uber.okbuck.rule.android.AndroidLibraryWrapperRule
import com.uber.okbuck.rule.base.BuckRule
import com.uber.okbuck.rule.base.GenRule

final class LintRuleComposer extends JvmBuckRuleComposer {

    private static final String BUCK_LINT_LIBRARIES = "buck_lint_libraries"

    private LintRuleComposer() {
        // no instance
    }

    static List<BuckRule> compose(JavaTarget target) {
        List<BuckRule> lintRules = []
        String lintConfigXml = ""
        if (target.lintOptions.lintConfig != null && target.lintOptions.lintConfig.exists()) {
            lintConfigXml = LintUtil.getLintwConfigRule(target.project, target.lintOptions.lintConfig)
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
        lintCmds += ["mkdir -p \$OUT;", "RUN_IN=`dirname ${toLocation(fileRule(target.manifest))}`;", "exec java", "-Djava.awt.headless=true"]

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

            Set<String> lintLibraries = external(target.lintLibraries.externalDeps) +
                    targets(target.lintLibraries.targetDeps)
            if (lintLibraries) {
                AndroidLibraryWrapperRule wrapperRule =
                        new AndroidLibraryWrapperRule(BUCK_LINT_LIBRARIES, lintLibraries as List)
                lintRules.add(wrapperRule)
                lintCmds.add("--libraries ${toClasspath(":" + BUCK_LINT_LIBRARIES)}")
            }
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
        if (lintConfigXml) {
            lintCmds.add("--config ${toLocation(lintConfigXml)}")
        }
        if (target.lintOptions.xmlReport) {
            lintCmds.add('--xml "\$OUT/lint-results.xml"')
        }
        if (target.lintOptions.htmlReport) {
            lintCmds.add('--html "\$OUT/lint-results.html"')
        }

        List<String> inputs = []
        target.main.sources.each { String sourceDir ->
            lintCmds.add("--sources ${sourceDir}")
            inputs.add(sourceDir)
        }
        if (target instanceof AndroidTarget) {
            target.resDirs.each { String resDir ->
                lintCmds.add("--resources ${resDir}")
                inputs.add(resDir)
            }

            // Project root is at okbuck generated manifest for this target
            lintCmds.add('$RUN_IN')
        }

        lintRules.add(new GenRule(
                lint(target),
                inputs,
                lintCmds))

        return lintRules
    }

    private static String lint(final JavaTarget target) {
        return "lint_" + target.getName();
    }
}
