package com.uber.okbuck.rule

final class LintRule extends BuckRule {

    static final String SEPARATOR = ':'

    private final String lintw
    private final Set<String> inputs
    private final Set<String> customLintTargets
    private final Set<String> lintDeps
    private final String lintTarget

    LintRule(String name,
             Set<String> inputs,
             String lintw,
             Set<String> customLintTargets,
             Set<String> lintDeps,
             String lintTarget) {
        super("genrule", name)
        this.lintw = lintw
        this.inputs = inputs
        this.customLintTargets = customLintTargets
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
        printer.println("\tbash = 'mkdir -p \$OUT; LINT_DEPS=${toClasspath(lintDeps)} ' \\")
        printer.println("\t'ANDROID_LINT_JARS=${toLocation(customLintTargets)} ' \\")
        if (lintTarget) {
            printer.println("\t'MODULE_JAR=${toLocation(lintTarget)} ' \\")
        }
        printer.println("\t'OUTPUT_DIR=\$OUT ' \\")
        printer.println("\t'${toLocation(lintw)}',")
    }

    static String toClasspath(Set<String> targets) {
        return (targets.collect { toClasspath(it) } as Set).join(SEPARATOR)
    }

    static String toLocation(Set<String> targets) {
        return (targets.collect { toLocation(it) } as Set).join(SEPARATOR)
    }

    static String toClasspath(String target) {
        return "\$(classpath ${target})"
    }

    static String toLocation(String target) {
        return "\$(location ${target})"
    }
}
