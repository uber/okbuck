package com.uber.okbuck.rule

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
