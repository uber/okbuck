package com.github.okbuilds.okbuck.rule

final class ProjectConfigRule extends BuckRule {
    private final String mSrcTarget;
    private final Set<String> mSrcRoots;
    private final String mTestTarget;
    private final Set<String> mTestRoots;

    ProjectConfigRule(String srcTarget, Set<String> srcRoots, String testTarget, Set<String> testRoots) {
        super("project_config", null)
        this.mSrcTarget = srcTarget
        this.mSrcRoots = srcRoots
        this.mTestTarget = testTarget
        this.mTestRoots = testRoots
    }

    @Override
    protected void printContent(PrintStream printer) {
        printer.println("\tsrc_target = ':${mSrcTarget}',")

        printer.println("\tsrc_roots = [")
        for (String src : mSrcRoots) {
            printer.println("\t\t'${src}',")
        }
        printer.println("\t],")

        if (mTestTarget != null) {
            printer.println("\ttest_target = ':${mTestTarget}',")
        }

        if (mTestRoots != null && !mTestRoots.empty) {
            printer.println("\ttest_roots = [")
            for (String src : mTestRoots) {
                printer.println("\t\t'${src}',")
            }
            printer.println("\t],")
        }
    }
}
