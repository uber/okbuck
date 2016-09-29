package com.uber.okbuck.rule

final class ZipRule extends BuckRule {

    private final Set<String> mSrcs
    private final Set<String> mSrcTargets

    ZipRule(String name, Set<String> srcs, Set<String> srcTargets) {
        super("zip_file", name)
        mSrcs = srcs
        mSrcTargets = srcTargets
    }

    @Override
    protected void printContent(PrintStream printer) {
        if (!mSrcs.empty) {
            printer.println("\tsrcs = glob([")
            for (String src : mSrcs) {
                printer.println("\t\t'${src}',")
            }
            if (!mSrcTargets.empty) {
                printer.println("\t]) + ")
            } else {
                printer.println("\t]),")
            }
        }
        if (!mSrcTargets.empty) {
            if (mSrcs.empty) {
                printer.println("\tsrcs = [")
            } else {
                printer.println("\t[")
            }
            for (String target : mSrcTargets) {
                printer.println("\t\t'${target}',")
            }
            printer.println("\t],")
        }
    }
}
