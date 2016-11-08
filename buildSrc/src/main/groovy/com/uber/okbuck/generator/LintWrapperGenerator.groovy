package com.uber.okbuck.generator

import com.uber.okbuck.core.model.AndroidTarget
import com.uber.okbuck.core.model.JavaTarget
import com.uber.okbuck.core.util.LintUtil

public class LintWrapperGenerator {

    private LintWrapperGenerator() {
        // no instance
    }

    static String generate(JavaTarget target, String lintJvmArgs) {
        File lintw = LintUtil.createLintw(target)
        String outputText = lintw.text
        outputText = outputText.replaceFirst('template-jvmArgs', lintJvmArgs)
        StringBuilder sb = new StringBuilder(outputText)

        if (!target.main.sources.empty) {
            sb.append(toCmd('--classpath "$LINT_TARGET"'))
        }
        if (target.lintOptions.abortOnError) {
            sb.append(toCmd("--exitcode"))
        }
        if (target.lintOptions.absolutePaths) {
            sb.append(toCmd("--fullpath"))
        }
        if (target.lintOptions.quiet) {
            sb.append(toCmd("--quiet"))
        }
        if (target.lintOptions.checkAllWarnings) {
            sb.append(toCmd("-Wall"))
        }
        if (target.lintOptions.ignoreWarnings) {
            sb.append(toCmd("--nowarn"))
        }
        if (target.lintOptions.warningsAsErrors) {
            sb.append(toCmd("-Werror"))
        }
        if (target.lintOptions.noLines) {
            sb.append(toCmd("--nolines"))
        }
        if (target.lintOptions.disable) {
            sb.append(toCmd("--disable ${target.lintOptions.disable.join(',')}"))
        }
        if (target.lintOptions.enable) {
            sb.append(toCmd("--enable ${target.lintOptions.enable.join(',')}"))
        }
        if (target.lintOptions.lintConfig && target.lintOptions.lintConfig.exists()) {
            sb.append(toCmd("--config ${target.lintOptions.lintConfig.absolutePath}"))
        }
        if (target.lintOptions.xmlReport) {
            sb.append(toCmd('--xml "${OUTPUT_DIR}/lint-results.xml"'))
        }
        if (target.lintOptions.htmlReport) {
            sb.append(toCmd('--html "${OUTPUT_DIR}/lint-results.html"'))
        }
        target.main.sources.each { String sourceDir ->
            sb.append(toCmd("--sources ${new File(target.project.projectDir, sourceDir).absolutePath}"))
        }
        if (target instanceof AndroidTarget) {
            target.getResources().each { AndroidTarget.ResBundle bundle ->
                if (bundle.resDir) {
                    sb.append(toCmd("--resources ${new File(target.project.projectDir, bundle.resDir).absolutePath}"))
                }
            }

            // Project root is at okbuck generated manifest for this target
            sb.append(toCmd("${new File(target.project.projectDir, target.manifest).parentFile.absolutePath}"))
        }

        lintw.text = sb.toString()
        lintw.setExecutable(true)

        return lintw.absolutePath
    }

    private static String toCmd(String option) {
        return "    ${option} \\\n"
    }
}
