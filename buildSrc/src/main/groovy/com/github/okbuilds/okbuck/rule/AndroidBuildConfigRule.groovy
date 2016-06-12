package com.github.okbuilds.okbuck.rule

import static com.github.okbuilds.core.util.CheckUtil.checkNotNull
import static com.github.okbuilds.core.util.CheckUtil.checkStringNotEmpty

final class AndroidBuildConfigRule extends BuckRule {

    private final String mPackage

    private final List<String> mValues

    AndroidBuildConfigRule(
            String name, List<String> visibility, String packageName,
            List<String> values
    ) {
        super("android_build_config", name, visibility)
        checkStringNotEmpty(packageName, "AndroidBuildConfigRule package can't be empty.")
        mPackage = packageName
        checkNotNull(values, "AndroidBuildConfigRule values must be non-null.")
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
