package com.uber.okbuck.rule

abstract class JavaRule extends BuckRule {
    private final Set<String> mSrcSet
    private final Set<String> mAnnotationProcessors
    private final Set<String> mAnnotationProcessorDeps
    private final String mResourcesDir
    private final String mSourceCompatibility
    private final String mTargetCompatibility
    private final List<String> mPostprocessClassesCommands
    private final List<String> mOptions
    private final Set<String> mProvidedDeps
    private final List<String> mTestTargets
    private final String mBootClasspath
    private final String mGenDir

    JavaRule(
            String ruleType,
            String name,
            List<String> visibility,
            List<String> deps,
            Set<String> srcSet,
            Set<String> annotationProcessors,
            Set<String> aptDeps,
            Set<String> providedDeps,
            String resourcesDir,
            String sourceCompatibility,
            String targetCompatibility,
            List<String> postprocessClassesCommands,
            List<String> options,
            List<String> testTargets,
            String bootClasspath,
            String genDir) {

        super(ruleType, name, visibility, deps)
        mSrcSet = srcSet
        mAnnotationProcessors = annotationProcessors
        mAnnotationProcessorDeps = aptDeps
        mSourceCompatibility = sourceCompatibility
        mTargetCompatibility = targetCompatibility
        mResourcesDir = resourcesDir
        mPostprocessClassesCommands = postprocessClassesCommands
        mOptions = options
        mProvidedDeps = providedDeps
        mTestTargets = testTargets
        mBootClasspath = bootClasspath
        mGenDir = genDir
    }

    @Override
    protected final void printContent(PrintStream printer) {
        if (!mSrcSet.empty) {
            printer.println("\tsrcs = glob([")
            for (String src : mSrcSet) {
                printer.println("\t\t'${src}/**/*.java',")
            }
            printer.println("\t]),")
        }

        if (mTestTargets) {
            printer.println("\ttests = [")
            for (String testTarget : mTestTargets) {
                printer.println("\t\t'${testTarget}',")
            }
            printer.println("\t],")
        }

        if (mResourcesDir) {
            printer.println("\tresources = glob([")
            printer.println("\t\t'${mResourcesDir}/**',")
            printer.println("\t]),")

            printer.println("\tresources_root = '${mResourcesDir}',")
        }

        if (!mAnnotationProcessors.empty) {
            printer.println("\tannotation_processors = [")
            mAnnotationProcessors.sort().each { String processor ->
                printer.println("\t\t'${processor}',")
            }
            printer.println("\t],")

            if (!mAnnotationProcessorDeps.empty) {
                printer.println("\tannotation_processor_deps = [")
                for (String dep : mAnnotationProcessorDeps.sort()) {
                    printer.println("\t\t'${dep}',")
                }
                printer.println("\t],")
            }
        }

        if (!mProvidedDeps.empty) {
            printer.println("\tprovided_deps = [")
            for (String dep : mProvidedDeps.sort()) {
                printer.println("\t\t'${dep}',")
            }
            printer.println("\t],")
        }

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
