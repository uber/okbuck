package com.uber.okbuck.rule

final class JavaBinaryRule extends BuckRule {

    private final String mMainClass

    JavaBinaryRule(String name, List<String> visibility, List<String> deps, String mainClass) {
        super("java_binary", name, visibility, deps)
        mMainClass = mainClass
    }

    @Override
    protected final void printContent(PrintStream printer) {
        if (mMainClass) {
            printer.println("\tmain_class = '${mMainClass}',")
        }
    }
}
