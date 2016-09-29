package com.uber.okbuck.rule

final class AndroidInstrumentationTestRule extends BuckRule {

    private final String mInstrumentationApkRuleName

    AndroidInstrumentationTestRule(String name, String instrumentationApkRuleName) {
        super("android_instrumentation_test", name, [], [])
        mInstrumentationApkRuleName = instrumentationApkRuleName
    }

    @Override
    protected final void printContent(PrintStream printer) {
        printer.println("\tapk = ':${mInstrumentationApkRuleName}',")
    }
}
