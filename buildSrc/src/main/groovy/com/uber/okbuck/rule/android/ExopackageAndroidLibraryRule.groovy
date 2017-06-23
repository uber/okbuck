package com.uber.okbuck.rule.android

import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.rule.base.BuckRule

final class ExopackageAndroidLibraryRule extends BuckRule {

    private final String mAppClass
    private final String mSourceCompatibility
    private final String mTargetCompatibility
    private final List<String> mPostprocessClassesCommands
    private final List<String> mOptions

    ExopackageAndroidLibraryRule(
                                 RuleType ruleType,
                                 String name,
                                 String appClass,
                                 List<String> visibility,
                                 List<String> deps,
                                 String sourceCompatibility,
                                 String targetCompatibility,
                                 List<String> postprocessClassesCommands,
                                 List<String> options,
                                 Set<String> extraOpts) {
        super(ruleType, name, visibility, deps, extraOpts)
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

        if (!mOptions.empty) {
            printer.println("\textra_arguments = [")
            mOptions.each { String option ->
                printer.println("\t\t'${option}',")
            }
            printer.println("\t],")
        }

        if (!mPostprocessClassesCommands.empty) {
            printer.println("\tpostprocess_classes_commands = [")
            mPostprocessClassesCommands.each { String cmd ->
                printer.println("\t\t'${cmd}',")
            }
            printer.println("\t],")
        }
    }
}
