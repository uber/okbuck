package com.uber.okbuck.experimental.bazel

import com.uber.okbuck.rule.BuckRule

class JavaLibraryRule extends BuckRule {
    private static final String RULE_TYPE = "java_library"

    private final Set<String> srcSet
    private final String resourcesDir
    private final Set<String> deps

    JavaLibraryRule(String name, Set<String> deps, Set<String> srcSet, String resourcesDir) {
        super(RULE_TYPE, name)
        this.srcSet = srcSet
        this.resourcesDir = resourcesDir
        this.deps = deps
    }

    @Override
    protected final void printContent(PrintStream printer) {
        if (!srcSet.empty) {
            printer.println("\tsrcs = glob([")
            srcSet.sort().each { String src -> printer.println("\t\t'${src}/**/*.java',")}
            printer.println("\t]),")
            printer.println("\tdeps = [")
            deps.sort().each { String dep -> printer.println("\t\t'${dep}',") }
            printer.println("\t],")
        } else {
            printer.println("\truntime_deps = [")
            deps.sort().each { String dep -> printer.println("\t\t'${dep}',") }
            printer.println("\t],")
        }
        if (resourcesDir) {
            printer.println("\tresources = glob(['${resourcesDir}']),)")
        }
    }
}
