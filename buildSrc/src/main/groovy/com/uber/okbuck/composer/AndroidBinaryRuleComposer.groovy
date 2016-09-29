package com.uber.okbuck.composer

import com.uber.okbuck.core.model.AndroidAppTarget
import com.uber.okbuck.rule.AndroidBinaryRule

final class AndroidBinaryRuleComposer extends AndroidBuckRuleComposer {

    private static Map<String, String> CPU_FILTER_MAP = [
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
                                     String keystoreRuleName) {
        Set<String> mappedCpuFilters = target.cpuFilters.collect { String cpuFilter ->
            CPU_FILTER_MAP.get(cpuFilter)
        }.findAll { String cpuFilter -> cpuFilter != null }

        return new AndroidBinaryRule(bin(target), ["PUBLIC"], deps, manifestRuleName, keystoreRuleName,
                target.multidexEnabled, target.linearAllocHardLimit, target.primaryDexPatterns,
                target.exopackage != null, mappedCpuFilters, target.minifyEnabled,
                target.proguardConfig, target.placeholders, target.extraOpts, target.includesVectorDrawables)
    }
}
