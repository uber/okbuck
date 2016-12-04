package com.uber.okbuck.rule.android

import com.uber.okbuck.rule.base.BuckRule

final class AndroidInstrumentationTestRule extends BuckRule {

    private final String mInstrumentationApkRuleName

    AndroidInstrumentationTestRule(String name, String instrumentationApkRuleName) {
        super("android_instrumentation_test", name, [], [])
        mInstrumentationApkRuleName = instrumentationApkRuleName
    }

    @Override
    protected final void printContent(PrintStream printer) {
        printer.println("\tapk = ':${mInstrumentationApkRuleName}',")
        printer.println("\tlabels = ['ui', 'android', 'device', 'espresso', 'instrumentation'],")
    }
}
