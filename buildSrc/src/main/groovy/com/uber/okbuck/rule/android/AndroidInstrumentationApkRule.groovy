package com.uber.okbuck.rule.android

import com.uber.okbuck.core.io.Printer
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.rule.base.BuckRule

final class AndroidInstrumentationApkRule extends BuckRule {

    private final String mManifest
    private final String mMainApkRuleName

    AndroidInstrumentationApkRule(String name, List<String> visibility, List<String> deps, String manifest, String
            mainApkRuleName) {
        super(RuleType.ANDROID_INSTRUMENTATION_APK, name, visibility, deps)

        mManifest = manifest
        mMainApkRuleName = mainApkRuleName
    }

    @Override
    protected final void printContent(Printer printer) {
        printer.println("\tmanifest = '${mManifest}',")
        printer.println("\tapk = ':${mMainApkRuleName}',")
    }
}
