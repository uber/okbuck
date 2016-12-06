package com.uber.okbuck.experimental.bazel

import com.uber.okbuck.rule.BuckRule

final class BazelAndroidBinaryRule extends BuckRule {
    private static final String RULE_TYPE = "android_binary"

    private final String packageName
    private final boolean multidexEnabled
    private final String manifest
    private final Map<String, Object> placeholders

    BazelAndroidBinaryRule(
            String name,
            String packageName,
            String androidLibrary,
            boolean multidexEnabled,
            String manifest,
            Map<String, Object> placeholders) {
        super(RULE_TYPE, name, [], [androidLibrary])
        this.packageName = packageName
        this.multidexEnabled = multidexEnabled
        this.manifest = manifest
        this.placeholders = placeholders
    }

    @Override
    protected final void printContent(PrintStream printer) {
        printer.println("\tcustom_package = '${packageName}',")
        if (manifest != null && !manifest.isEmpty()) {
            printer.println("\tmanifest = '${manifest}',")
        }
        if (multidexEnabled) {
            printer.println("\tmultidex = 'native',")
        }
        if (!placeholders.isEmpty()) {
            printer.println("\tmanifest_values = {")
            placeholders.each { key, value -> printer.println("\t\t'${key}': '${value}',") }
            printer.println("\t},")
        }
    }
}
