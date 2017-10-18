package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.template.android.AndroidBinaryRule
import com.uber.okbuck.template.core.Rule

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

    static Rule compose(AndroidAppTarget target, List<String> deps, String manifestRuleName,
                        String keystoreRuleName, List<Rule> transformGenRules = []) {
        Set<String> mappedCpuFilters = target.cpuFilters.collect { String cpuFilter ->
            CPU_FILTER_MAP.get(cpuFilter)
        }.findAll { String cpuFilter -> cpuFilter != null }

        Set<String> transformRuleNames = transformGenRules.collect {
            ":${it.name()}"
        }

        String bashCommand = transformRuleNames.collect {
            "\$(exe ${it}) \$IN_JARS_DIR \$OUT_JARS_DIR \$ANDROID_BOOTCLASSPATH;"
        }.join(" ")

        List<String> testTargets = []
        if (target.instrumentationTarget) {
            testTargets.add(":${instrumentationTest(target)}")
        }

        String proguardConfig = target.proguardConfig
        if (proguardConfig && target.proguardMapping) {
            deps.add(fileRule(target.proguardMapping))
        }

        return new AndroidBinaryRule()
                .manifest(manifestRuleName)
                .keystore(keystoreRuleName)
                .multidexEnabled(target.multidexEnabled)
                .linearAllocHardLimit(target.linearAllocHardLimit)
                .primaryDexPatterns(target.primaryDexPatterns)
                .exopackage(target.exopackage != null)
                .cpuFilters(mappedCpuFilters)
                .minifyEnabled(target.minifyEnabled)
                .proguardConfig(fileRule(proguardConfig))
                .placeholders(target.placeholders)
                .includesVectorDrawables(target.includesVectorDrawables)
                .preprocessJavaClassesDeps(transformRuleNames)
                .preprocessJavaClassesBash(bashCommand)
                .testTargets(testTargets)
                .ruleType(RuleType.ANDROID_BINARY.getBuckName())
                .defaultVisibility()
                .deps(deps)
                .name(bin(target))
                .extraBuckOpts(target.getExtraOpts(RuleType.ANDROID_BINARY))
    }
}
