package com.uber.okbuck.rule.android

import com.uber.okbuck.rule.base.BuckRule

final class AndroidBuildConfigRule extends BuckRule {

    private final String mPackage

    private final List<String> mValues

    AndroidBuildConfigRule(
            String name, List<String> visibility, String packageName,
            List<String> values
    ) {
        super("android_build_config", name, visibility)
        mPackage = packageName
        mValues = values
    }

    @Override
    protected final void printContent(PrintStream printer) {
        printer.println("\tpackage = '${mPackage}',")
        if (!mValues.empty) {
            printer.println("\tvalues = [")
            for (String value : mValues) {
                printer.println("\t\t'${value}',")
            }
            printer.println("\t],")
        }
    }
}
