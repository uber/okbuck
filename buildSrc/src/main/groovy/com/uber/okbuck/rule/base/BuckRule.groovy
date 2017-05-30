package com.uber.okbuck.rule.base

import com.uber.okbuck.core.model.base.RuleType

abstract class BuckRule {

    final String name
    private final String mRuleType
    private final Set<String> mVisibility
    private final Set<String> mDeps
    private final Set<String> mExtraBuckOpts

    BuckRule(RuleType ruleType, String name, List<String> visibility = [], List<String> deps = [],
             Set<String> extraBuckOpts = []) {
        this.name = name
        // TODO clean this up, the rule name generation is a bit hacky
        mRuleType = ruleType == RuleType.KOTLIN_ANDROID_LIBRARY ? "android_library" : ruleType.name().toLowerCase()
        mVisibility = new LinkedHashSet(visibility)
        mDeps = new LinkedHashSet(deps) // de-dup dependencies
        mExtraBuckOpts = extraBuckOpts
    }

    /**
     * Print this rule into the printer.
     */
    void print(PrintStream printer) {
        printer.println("${mRuleType}(")

        if (name != null) {
            printer.println("\tname = '${name}',")
        }
        printContent(printer)
        mExtraBuckOpts.each { String option ->
            printer.println("\t${option},")
        }
        if (!mDeps.empty) {
            printer.println("\tdeps = [")
            mDeps.sort().each { String dep ->
                printer.println("\t\t'${dep}',")
            }
            printer.println("\t],")
        }
        if (!mVisibility.empty) {
            printer.println("\tvisibility = [")
            for (String visibility : mVisibility) {
                printer.println("\t\t'${visibility}',")
            }
            printer.println("\t],")
        }
        printer.println(")")
        printer.println()
    }

    /**
     * Print rule content.
     *
     * @param printer The printer.
     */
    protected abstract void printContent(PrintStream printer)
}
