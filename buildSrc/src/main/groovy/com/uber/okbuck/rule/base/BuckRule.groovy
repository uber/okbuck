package com.uber.okbuck.rule.base

abstract class BuckRule {

    final String name
    private final String mRuleType
    private final Set<String> mVisibility
    private final Set<String> mDeps

    BuckRule(String ruleType, String name, List<String> visibility = [], List<String> deps = []) {
        this.name = name
        mRuleType = ruleType
        mVisibility = new LinkedHashSet(visibility)
        mDeps = new LinkedHashSet(deps) // de-dup dependencies
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
