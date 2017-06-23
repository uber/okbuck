package com.uber.okbuck.rule.android

import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.core.model.jvm.TestOptions
import com.uber.okbuck.rule.java.JavaRule
import org.apache.commons.lang.StringUtils

abstract class AndroidRule extends JavaRule {

    private final RuleType mRuleType
    private final String mManifest
    private final String mRobolectricManifest
    private final List<String> mAidlRuleNames
    private final boolean mGenerateR2
    private final String mRuntimeDependency

    AndroidRule(
            RuleType ruleType,
            String name,
            List<String> visibility,
            List<String> deps,
            Set<String> srcSet,
            String manifest,
            String robolectricManifest,
            List<String> annotationProcessors,
            List<String> aptDeps,
            Set<String> providedDeps,
            List<String> aidlRuleNames,
            String appClass,
            String sourceCompatibility,
            String targetCompatibility,
            List<String> postprocessClassesCommands,
            List<String> options,
            TestOptions testOptions,
            boolean generateR2,
            String resourcesDir,
            String runtimeDependency,
            List<String> testTargets,
            List<String> labels = null,
            Set<String> extraOpts = []) {
        super(ruleType, name, visibility, deps, srcSet, annotationProcessors, aptDeps, providedDeps,
                resourcesDir, sourceCompatibility, targetCompatibility, postprocessClassesCommands,
                options, testOptions, testTargets, labels, extraOpts,
                !StringUtils.isEmpty(appClass)
                        ? Collections.singleton(appClass) : Collections.emptySet())

        mRuleType = ruleType
        mManifest = manifest
        mRobolectricManifest = robolectricManifest
        mAidlRuleNames = aidlRuleNames
        mGenerateR2 = generateR2
        mRuntimeDependency = runtimeDependency
    }

    @Override
    protected final void printContent(PrintStream printer) {
        super.printContent(printer)

        if (mRuleType == RuleType.ANDROID_LIBRARY_WITH_KOTLIN ||
            mRuleType == RuleType.ROBOLECTRIC_TEST_WITH_KOTLIN) {
            printer.println("\tlanguage = 'kotlin',")
        }

        if (!StringUtils.isEmpty(mManifest)) {
            printer.println("\tmanifest = '${mManifest}',")
        }

        if (!StringUtils.isEmpty(mRobolectricManifest)) {
            printer.println("\trobolectric_manifest = '${mRobolectricManifest}',")
        }

        if (!mAidlRuleNames.empty) {
            printer.println("\texported_deps = [")
            mAidlRuleNames.sort().each { String aidlRuleName ->
                printer.println("\t\t'${aidlRuleName}',")
            }
            printer.println("\t],")
        }

        if (mGenerateR2) {
            printer.println("\tfinal_r_name = 'R2',")
        }

        if (mRuntimeDependency) {
            printer.println("\trobolectric_runtime_dependency = '${mRuntimeDependency}',")
        }
    }
}
