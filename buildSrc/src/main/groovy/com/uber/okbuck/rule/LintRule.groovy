package com.uber.okbuck.rule

final class LintRule extends BuckRule {

    static final String SEPARATOR = ':'

    private final String lintw
    private final Set<String> inputs
    private final Set<String> customLintRules
    private final Set<String> lintDeps
    private final String lintTarget

    LintRule(String name,
             Set<String> inputs,
             String lintw,
             Set<String> customLintRules,
             Set<String> lintDeps,
             String lintTarget) {
        super("genrule", name)
        this.lintw = lintw
        this.inputs = inputs
        this.customLintRules = customLintRules
        this.lintDeps = lintDeps
        this.lintTarget = lintTarget
    }

    @Override
    final void printContent(PrintStream printer) {
        if (!inputs.empty) {
            printer.println("\tsrcs = [")
            for (String input : inputs) {
                printer.println("\t\t'${input}',")
            }
            printer.println("\t],")
        }

        printer.println("\tout = '${name}_out',")
        printer.println("\tbash = 'mkdir -p \$OUT ; ' \\")
        if (lintDeps) {
            printer.println("\t'export JAR_PATH=\"${toLocation(lintDeps)}\" ; ' \\")
        }
        if (customLintRules) {
            printer.println("\t'export ANDROID_LINT_JARS=\"${toLocation(customLintRules)}\" ; ' \\")
        }
        if (lintTarget) {
            printer.println("\t'export LINT_TARGET=${toLocation(lintTarget)} ; ' \\")
        }
        printer.println("\t'export OUTPUT_DIR=\$OUT ; ' \\")
        printer.println("\t'${toLocation(lintw)}',")
    }

    static String toLocation(Set<String> targets) {
        return (targets.collect { toLocation(it) } as Set).join(SEPARATOR)
    }

    static String toLocation(String target) {
        return "\$(location ${target})"
    }
}
