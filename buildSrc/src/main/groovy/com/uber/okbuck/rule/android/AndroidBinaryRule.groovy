package com.uber.okbuck.rule.android

import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.rule.base.BuckRule

final class AndroidBinaryRule extends BuckRule {

    private final String mManifest
    private final String mKeystore
    private final boolean mMultidexEnabled
    private final int mLinearAllocHardLimit
    private final Set<String> mPrimaryDexPatterns
    private final boolean mExopackage
    private final Set<String> mCpuFilters
    private final boolean mMinifyEnabled
    private final String mProguardConfig
    private final Map<String, Object> mPlaceholders
    private final boolean mIncludesVectorDrawables
    private final Set<String> mPreprocessJavaClassesDeps
    private final String mPreprocessJavaClassesBash

    AndroidBinaryRule(String name, List<String> visibility, List<String> deps, String manifest, String keystore,
                      boolean multidexEnabled, int linearAllocHardLimit, Set<String> primaryDexPatterns,
                      boolean exopackage, Set<String> cpuFilters, boolean minifyEnabled,
                      String proguardConfig, Map<String, Object> placeholders, Set<String> extraOpts,
                      boolean includesVectorDrawables, Set<String> preprocessJavaClassesDeps,
                      String preprocessJavaClassesBash) {
        super(RuleType.ANDROID_BINARY, name, visibility, deps, extraOpts)

        mManifest = manifest
        mKeystore = keystore
        mMultidexEnabled = multidexEnabled
        mLinearAllocHardLimit = linearAllocHardLimit
        mPrimaryDexPatterns = primaryDexPatterns
        mExopackage = exopackage
        mCpuFilters = cpuFilters
        mMinifyEnabled = minifyEnabled
        mProguardConfig = proguardConfig
        mPlaceholders = placeholders
        mIncludesVectorDrawables = includesVectorDrawables
        mPreprocessJavaClassesDeps = preprocessJavaClassesDeps
        mPreprocessJavaClassesBash = preprocessJavaClassesBash
    }

    @Override
    protected final void printContent(PrintStream printer) {
        printer.println("\tmanifest = '${mManifest}',")
        printer.println("\tkeystore = '${mKeystore}',")
        if (mExopackage) {
            printer.println("\texopackage_modes = ['secondary_dex'],")
        }
        if (mMultidexEnabled && mPrimaryDexPatterns != null) {
            printer.println("\tuse_split_dex = True,")
            printer.println("\tlinear_alloc_hard_limit = ${mLinearAllocHardLimit},")
            if (mPrimaryDexPatterns != null && !mPrimaryDexPatterns.empty) {
                printer.println("\tprimary_dex_patterns = [")
                for (String pattern : mPrimaryDexPatterns) {
                    printer.println("\t\t'${pattern}',")
                }
                printer.println("\t],")
            }
        }
        if (mPreprocessJavaClassesDeps) {
            printer.println("\tpreprocess_java_classes_deps = ['${mPreprocessJavaClassesDeps.join("','")}'],")
        }
        if (mPreprocessJavaClassesBash) {
            printer.println("\tpreprocess_java_classes_bash = '${mPreprocessJavaClassesBash}',")
        }
        if (mCpuFilters != null && !mCpuFilters.empty) {
            printer.println("\tcpu_filters = [")
            for (String filter : mCpuFilters) {
                printer.println("\t\t'${filter}',")
            }
            printer.println("\t],")
        }
        if (mMinifyEnabled) {
            printer.println("\tpackage_type = 'release',")
            printer.println("\tandroid_sdk_proguard_config = 'none',")
            printer.println("\tproguard_config = '${mProguardConfig}',")
        }

        if (!mPlaceholders.isEmpty()) {
            printer.println("\tmanifest_entries = {")
            printer.println("\t\t'placeholders': {")
            mPlaceholders.each { key, value ->
                printer.println("\t\t\t'${key}': '${value}',")
            }
            printer.println("\t\t},")
            printer.println("\t},")
        }

        if (mIncludesVectorDrawables) {
            printer.println("\tincludes_vector_drawables = True,")
        }
    }
}
