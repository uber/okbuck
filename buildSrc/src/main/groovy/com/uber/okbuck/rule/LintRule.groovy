package com.uber.okbuck.rule

final class LintRule extends BuckRule {

    static final String SEPARATOR = ':'

    private final String lintw
    private final Set<String> inputs
    private final Set<String> customLintTargets
    private final Set<String> classpathLintDeps
    private final Set<String> locationLintDeps
    private final String lintTarget

    LintRule(String name,
             Set<String> inputs,
             String lintw,
             Set<String> customLintTargets,
             Set<String> classpathLintDeps,
             Set<String> locationLintDeps,
             String lintTarget) {
        super("genrule", name)
        this.lintw = lintw
        this.inputs = inputs
        this.customLintTargets = customLintTargets
        this.classpathLintDeps = classpathLintDeps
        this.locationLintDeps = locationLintDeps
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

        String lintDeps = [toClasspath(classpathLintDeps), toLocation(locationLintDeps)].findAll {
            !it.empty
        }.join(SEPARATOR)

        printer.println("\tout = '${name}_out',")
        printer.println("\tbash = 'mkdir -p \$OUT; export LINT_DEPS=${lintDeps} ; ' \\")
        printer.println("\t'export ANDROID_LINT_JARS=${toLocation(customLintTargets)} ; ' \\")
        if (lintTarget) {
            printer.println("\t'export MODULE_JAR=${toLocation(lintTarget)} ; ' \\")
        }
        printer.println("\t'export OUTPUT_DIR=\$OUT ; ' \\")
        printer.println("\t'${toLocation(lintw)}',")
    }

    static String toLintDeps(Set<String> targets) {
        return (targets.collect { toClasspath(it) } as Set).join(SEPARATOR)
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
