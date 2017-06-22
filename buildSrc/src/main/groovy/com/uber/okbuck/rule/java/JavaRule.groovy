package com.uber.okbuck.rule.java

import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.core.model.jvm.TestOptions
import com.uber.okbuck.rule.base.BuckRule

abstract class JavaRule extends BuckRule {

    private final Set<String> mSrcSet
    private final Set<String> mAnnotationProcessors
    private final Set<String> mAnnotationProcessorDeps
    private final String mResourcesDir
    private final String mSourceCompatibility
    private final String mTargetCompatibility
    private final List<String> mPostprocessClassesCommands
    private final List<String> mOptions
    private final TestOptions mTestOptions
    private final Set<String> mProvidedDeps
    private final List<String> mTestTargets
    private final List<String> mLabels
    private final Set<String> mExcludes
    private final Set<String> mSourceExtensions

    JavaRule(
            RuleType ruleType,
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
            TestOptions testOptions,
            List<String> testTargets,
            List<String> labels = null,
            Set<String> extraOpts = [],
            Set<String> excludes = []) {

        super(ruleType, name, visibility, deps, extraOpts)
        mSrcSet = srcSet
        mAnnotationProcessors = annotationProcessors
        mAnnotationProcessorDeps = aptDeps
        mSourceCompatibility = sourceCompatibility
        mTargetCompatibility = targetCompatibility
        mResourcesDir = resourcesDir
        mPostprocessClassesCommands = postprocessClassesCommands
        mOptions = options
        mTestOptions = testOptions
        mProvidedDeps = providedDeps
        mTestTargets = testTargets
        mLabels = labels
        mExcludes = excludes
        mSourceExtensions = ruleType.getSourceExtensions()
    }

    @Override
    protected void printContent(PrintStream printer) {
        if (!mSrcSet.empty) {
            printer.println("\tsrcs = glob([")
            for (String src : mSrcSet) {
                for (String ext: mSourceExtensions) {
                    printer.println("\t\t'${src}/**/*.${ext}',")
                }
            }

            if (!mExcludes.empty) {
                printer.println("\t], excludes = [")
                for (String exclude : mExcludes) {
                    printer.println("\t\t'${exclude}',")
                }
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
        }

        if (!mAnnotationProcessorDeps.empty) {
            printer.println("\tannotation_processor_deps = [")
            for (String dep : mAnnotationProcessorDeps.sort()) {
                printer.println("\t\t'${dep}',")
            }
            printer.println("\t],")
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

        if (mLabels) {
            printer.println("\tlabels = [")
            mLabels.each { String label ->
                printer.println("\t\t'${label}',")
            }
            printer.println("\t],")
        }

        if (mTestOptions.jvmArgs) {
            printer.println("\tvm_args = [")
            mTestOptions.jvmArgs.each { String arg ->
                printer.println("\t\t'${arg}',")
            }
            printer.println("\t],")
        }

        if (mTestOptions.env) {
            printer.println("\tenv = {")
            mTestOptions.env.each { String key, Object value ->
                printer.println("\t\t'${key}': '${value.toString()}',")
            }
            printer.println("\t},")
        }
    }
}
