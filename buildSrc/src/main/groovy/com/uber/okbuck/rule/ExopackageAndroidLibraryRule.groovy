package com.uber.okbuck.rule

final class ExopackageAndroidLibraryRule extends BuckRule {
    private final String mAppClass
    private final String mSourceCompatibility
    private final String mTargetCompatibility
    private final List<String> mPostprocessClassesCommands
    private final List<String> mOptions
    private final String mBootClasspath
    private final String mGenDir

    ExopackageAndroidLibraryRule(String name,
                                 String appClass,
                                 List<String> visibility,
                                 List<String> deps,
                                 String sourceCompatibility,
                                 String targetCompatibility,
                                 List<String> postprocessClassesCommands,
                                 List<String> options,
                                 String bootClasspath,
                                 String genDir) {
        super("android_library", name, visibility, deps)
        mAppClass = appClass
        mSourceCompatibility = sourceCompatibility
        mTargetCompatibility = targetCompatibility
        mPostprocessClassesCommands = postprocessClassesCommands
        mOptions = options
        mBootClasspath = bootClasspath
        mGenDir = genDir
    }

    @Override
    protected final void printContent(PrintStream printer) {
        printer.println("\tsrcs = ['${mAppClass}'],")
        printer.println("\tsource = '${mSourceCompatibility}',")
        printer.println("\ttarget = '${mTargetCompatibility}',")
        if (!mPostprocessClassesCommands.empty) {
            String deps = "\$(JARS=(`find ${mGenDir} ! -name \"*-abi.jar\" ! -name \"*dex.dex.jar\" -name \"*.jar\"`); IFS=:; echo \"\${JARS[*]}\")"
            String androidJar = mBootClasspath
            printer.println("\tpostprocess_classes_commands = [")
            mPostprocessClassesCommands.each { String command ->
                printer.println("\t\t'export DEPS=${deps}; export ANDROID_JAR=${androidJar}; ${command}',")
            }
            printer.println("\t],")
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
