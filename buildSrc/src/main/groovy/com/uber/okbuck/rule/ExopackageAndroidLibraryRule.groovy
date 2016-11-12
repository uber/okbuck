package com.uber.okbuck.rule

final class ExopackageAndroidLibraryRule extends BuckRule {

    private final String mAppClass
    private final String mSourceCompatibility
    private final String mTargetCompatibility
    private final String mJavac
    private final List<String> mOptions

    ExopackageAndroidLibraryRule(String name,
                                 String appClass,
                                 List<String> visibility,
                                 List<String> deps,
                                 String sourceCompatibility,
                                 String targetCompatibility,
                                 String javac,
                                 List<String> options) {
        super("android_library", name, visibility, deps)
        mAppClass = appClass
        mSourceCompatibility = sourceCompatibility
        mTargetCompatibility = targetCompatibility
        mJavac = javac
        mOptions = options
    }

    @Override
    protected final void printContent(PrintStream printer) {
        if (mJavac) {
            printer.println("\tsrcs = ['${mAppClass}', '${mJavac}'],")
        } else {
            printer.println("\tsrcs = ['${mAppClass}'],")
        }
        printer.println("\tsource = '${mSourceCompatibility}',")
        printer.println("\ttarget = '${mTargetCompatibility}',")

        if (mJavac) {
            printer.println("\tjavac = '${mJavac}',")
        }

        if (!mOptions.empty) {
            printer.println("\textra_arguments = [")
            mOptions.each { String option ->
                printer.println("\t\t'${option}',")
            }
            printer.println("\t],")
        }
    }
}
