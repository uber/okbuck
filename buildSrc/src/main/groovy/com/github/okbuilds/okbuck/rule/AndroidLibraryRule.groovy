package com.github.okbuilds.okbuck.rule

import org.apache.commons.lang.StringUtils

final class AndroidLibraryRule extends BuckRule {
    private final Set<String> mSrcSet
    private final String mManifest
    private final List<String> mAnnotationProcessors
    private final List<String> mAptDeps
    private final List<String> mAidlRuleNames
    private final String mAppClass
    private final String mSourceCompatibility
    private final String mTargetCompatibility
    private final List<String> mPostprocessClassesCommands
    private final List<String> mOptions
    private final Set<String> mProvidedDeps

    /**
     * @param appClass , if exopackage is enabled, pass the detected app class, otherwise, pass null
     * */
    AndroidLibraryRule(
            String name,
            List<String> visibility,
            List<String> deps,
            Set<String> srcSet,
            String manifest,
            List<String> annotationProcessors,
            List<String> aptDeps,
            Set<String> providedDeps,
            List<String> aidlRuleNames,
            String appClass,
            String sourceCompatibility,
            String targetCompatibility,
            List<String> postprocessClassesCommands,
            List<String> options) {
        super("android_library", name, visibility, deps)

        mSrcSet = srcSet
        mManifest = manifest
        mAnnotationProcessors = annotationProcessors
        mAptDeps = aptDeps
        mAidlRuleNames = aidlRuleNames
        mAppClass = appClass
        mSourceCompatibility = sourceCompatibility
        mTargetCompatibility = targetCompatibility
        mPostprocessClassesCommands = postprocessClassesCommands
        mOptions = options
        mProvidedDeps = providedDeps
    }

    @Override
    protected final void printContent(PrintStream printer) {
        printer.println("\tsrcs = glob([")
        for (String src : mSrcSet) {
            printer.println("\t\t'${src}/**/*.java',")
        }

        if (!StringUtils.isEmpty(mAppClass)) {
            printer.println("\t], excludes = ['${mAppClass}']),")
        } else {
            printer.println("\t]),")
        }

        if (!StringUtils.isEmpty(mManifest)) {
            printer.println("\tmanifest = '${mManifest}',")
        }

        if (!mAnnotationProcessors.empty) {
            printer.println("\tannotation_processors = [")
            for (String processor : mAnnotationProcessors) {
                printer.println("\t\t'${processor}',")
            }
            printer.println("\t],")
        }

        if (!mAptDeps.empty) {
            printer.println("\tannotation_processor_deps = [")
            for (String dep : mAptDeps) {
                printer.println("\t\t'${dep}',")
            }
            printer.println("\t],")
        }

        if (!mProvidedDeps.empty) {
            printer.println("\tprovided_deps = [")
            for (String dep : mProvidedDeps) {
                printer.println("\t\t'${dep}',")
            }
            printer.println("\t],")
        }

        if (!mAidlRuleNames.empty) {
            printer.println("\texported_deps = [")
            mAidlRuleNames.each { String aidlRuleName ->
                printer.println("\t\t'${aidlRuleName}',")
            }
            printer.println("\t],")
        }

        printer.println("\tsource = '${mSourceCompatibility}',")
        printer.println("\ttarget = '${mTargetCompatibility}',")
        if (!mPostprocessClassesCommands.empty) {
            printer.println("\tpostprocess_classes_commands = [")
            mPostprocessClassesCommands.each { String command ->
                printer.println("\t\t'${command}',")
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
