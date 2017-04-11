package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.rule.android.AndroidBinaryRule
import com.uber.okbuck.rule.base.GenRule

final class AndroidBinaryRuleComposer extends AndroidBuckRuleComposer {

    private static final Map<String, String> CPU_FILTER_MAP = [
            "armeabi"    : "ARM",
            "armeabi-v7a": "ARMV7",
            "x86"        : "X86",
            "x86_64"     : "X86_64",
            "mips"       : "MIPS",
    ]

    private AndroidBinaryRuleComposer() {
        // no instance
    }

    static AndroidBinaryRule compose(AndroidAppTarget target, List<String> deps, String manifestRuleName,
                                     String keystoreRuleName, List<GenRule> transformGenRules = []) {
        Set<String> mappedCpuFilters = target.cpuFilters.collect { String cpuFilter ->
            CPU_FILTER_MAP.get(cpuFilter)
        }.findAll { String cpuFilter -> cpuFilter != null }

        Set<String> transformRuleNames = transformGenRules.collect {
            ":${it.name}"
        }

        String bashCommand = transformRuleNames.collect {
            "\$(exe ${it}) \$IN_JARS_DIR \$OUT_JARS_DIR \$ANDROID_BOOTCLASSPATH;"
        }.join(" ")
        return new AndroidBinaryRule(bin(target), ["PUBLIC"], deps, manifestRuleName, keystoreRuleName,
                target.multidexEnabled, target.linearAllocHardLimit, target.primaryDexPatterns,
                target.exopackage != null, mappedCpuFilters, target.minifyEnabled,
                fileRule(target.proguardConfig), target.placeholders, target.getExtraOpts(RuleType.ANDROID_BINARY),
                target.includesVectorDrawables, transformRuleNames, bashCommand)
    }
}
