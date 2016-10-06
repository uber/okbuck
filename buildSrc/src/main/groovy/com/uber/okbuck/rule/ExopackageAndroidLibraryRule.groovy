package com.uber.okbuck.rule

import com.uber.okbuck.block.PostProcessClassessCommands

final class ExopackageAndroidLibraryRule extends BuckRule {
    private final String mAppClass
    private final String mSourceCompatibility
    private final String mTargetCompatibility
    private final PostProcessClassessCommands mPostprocessClassesCommands
    private final List<String> mOptions

    ExopackageAndroidLibraryRule(String name,
                                 String appClass,
                                 List<String> visibility,
                                 List<String> deps,
                                 String sourceCompatibility,
                                 String targetCompatibility,
                                 PostProcessClassessCommands postprocessClassesCommands,
                                 List<String> options) {
        super("android_library", name, visibility, deps)
        mAppClass = appClass
        mSourceCompatibility = sourceCompatibility
        mTargetCompatibility = targetCompatibility
        mPostprocessClassesCommands = postprocessClassesCommands
        mOptions = options
    }

    @Override
    protected final void printContent(PrintStream printer) {
        printer.println("\tsrcs = ['${mAppClass}'],")
        printer.println("\tsource = '${mSourceCompatibility}',")
        printer.println("\ttarget = '${mTargetCompatibility}',")
        mPostprocessClassesCommands.println(printer)

        if (!mOptions.empty) {
            printer.println("\textra_arguments = [")
            mOptions.each { String option ->
                printer.println("\t\t'${option}',")
            }
            printer.println("\t],")
        }
    }
}
