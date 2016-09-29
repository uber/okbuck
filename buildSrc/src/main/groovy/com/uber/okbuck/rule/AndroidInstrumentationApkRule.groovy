package com.uber.okbuck.rule

final class AndroidInstrumentationApkRule extends BuckRule {

    private final String mManifest
    private final String mMainApkRuleName

    AndroidInstrumentationApkRule(String name, List<String> visibility, List<String> deps, String manifest, String
            mainApkRuleName) {
        super("android_instrumentation_apk", name, visibility, deps)

        mManifest = manifest
        mMainApkRuleName = mainApkRuleName
    }

    @Override
    protected final void printContent(PrintStream printer) {
        printer.println("\tmanifest = '${mManifest}',")
        printer.println("\tapk = ':${mMainApkRuleName}',")
    }
}
