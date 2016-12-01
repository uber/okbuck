package com.uber.okbuck.composer

import com.uber.okbuck.core.model.AndroidAppTarget
import com.uber.okbuck.core.util.TransformUtil
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

        Set<String> transformRules = TransformUtil.getTransformRules(target)
        String bashCommand = null;
        if (transformRules != null && !transformRules.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String transformRule : transformRules) {
                sb.append("\$(exe ${transformRule}) \$IN_JARS_DIR \$OUT_JARS_DIR \$ANDROID_BOOTCLASSPATH;")
            }
            bashCommand = sb.toString()
        }
        return new AndroidBinaryRule(bin(target), ["PUBLIC"], deps, manifestRuleName, keystoreRuleName,
                target.multidexEnabled, target.linearAllocHardLimit, target.primaryDexPatterns,
                target.exopackage != null, mappedCpuFilters, target.minifyEnabled,
                target.proguardConfig, target.placeholders, target.extraOpts, target.includesVectorDrawables,
                transformRules, bashCommand)
    }
}
