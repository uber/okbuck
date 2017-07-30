package com.uber.okbuck.rule.android

import com.uber.okbuck.core.io.Printer
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.rule.base.BuckRule

final class PrebuiltNativeLibraryRule extends BuckRule {

    private final String mNativeLibs

    PrebuiltNativeLibraryRule(String name, List<String> visibility, String nativeLibs) {
        super(RuleType.PREBUILT_NATIVE_LIBRARY, name, visibility)
        mNativeLibs = nativeLibs
    }

    @Override
    protected final void printContent(Printer printer) {
        printer.println("\tnative_libs = '${mNativeLibs}',")
    }
}
