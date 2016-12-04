package com.uber.okbuck.rule.android

import com.uber.okbuck.rule.base.BuckRule

final class PrebuiltNativeLibraryRule extends BuckRule {

    private final String mNativeLibs

    PrebuiltNativeLibraryRule(String name, List<String> visibility, String nativeLibs) {
        super("prebuilt_native_library", name, visibility)
        mNativeLibs = nativeLibs
    }

    @Override
    protected final void printContent(PrintStream printer) {
        printer.println("\tnative_libs = '${mNativeLibs}',")
    }
}
