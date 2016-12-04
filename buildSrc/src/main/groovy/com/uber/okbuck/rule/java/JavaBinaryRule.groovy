package com.uber.okbuck.rule.java

import com.uber.okbuck.rule.base.BuckRule

final class JavaBinaryRule extends BuckRule {

    private final String mMainClass
    private final Set<String> mExcludes

    JavaBinaryRule(String name,
                   List<String> visibility,
                   List<String> deps,
                   String mainClass,
                   Set<String> excludes = []) {
        super("java_binary", name, visibility, deps)
        mMainClass = mainClass
        mExcludes = excludes
    }

    @Override
    protected final void printContent(PrintStream printer) {
        if (mMainClass) {
            printer.println("\tmain_class = '${mMainClass}',")
        }
        if (!mExcludes.empty) {
            printer.println("\tblacklist = [")
            for (String exclude : mExcludes) {
                printer.println("\t\t'${exclude}',")
            }
            printer.println("\t],")
        }
    }
}
