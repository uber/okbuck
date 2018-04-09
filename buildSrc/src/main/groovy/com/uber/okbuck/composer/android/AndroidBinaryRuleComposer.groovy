package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.core.util.TransformUtil
import com.uber.okbuck.template.android.AndroidBinaryRule
import com.uber.okbuck.template.core.Rule
import org.apache.commons.lang3.tuple.Pair

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

    static Rule compose(AndroidAppTarget target, List<String> deps, String keystoreRuleName) {
        Set<String> mappedCpuFilters = target.cpuFilters.collect { String cpuFilter ->
            CPU_FILTER_MAP.get(cpuFilter)
        }.findAll { String cpuFilter -> cpuFilter != null }

        Pair<String, List<String>> results = TransformUtil.getBashCommandAndTransformDeps(target)
        String bashCommand = results.left
        List<String> transformDeps = results.right
        transformDeps.add(TransformUtil.TRANSFORM_RULE)

        List<String> testTargets = []
        if (target.appInstrumentationTarget) {
            testTargets.add(":${instrumentationTest(target)}")
        }

        String proguardConfig = target.proguardConfig
        if (proguardConfig && target.proguardMapping) {
            deps.add(fileRule(target.proguardMapping))
        }

        return new AndroidBinaryRule()
                .manifestSkeleton(fileRule(target.manifest))
                .keystore(keystoreRuleName)
                .multidexEnabled(target.multidexEnabled)
                .linearAllocHardLimit(target.linearAllocHardLimit)
                .primaryDexPatterns(target.primaryDexPatterns)
                .exopackage(target.exopackage != null)
                .cpuFilters(mappedCpuFilters)
                .minifyEnabled(target.minifyEnabled)
                .proguardConfig(fileRule(proguardConfig))
                .debuggable(target.debuggable)
                .placeholders(target.placeholders)
                .includesVectorDrawables(target.includesVectorDrawables)
                .preprocessJavaClassesDeps(transformDeps)
                .preprocessJavaClassesBash(bashCommand)
                .testTargets(testTargets)
                .ruleType(RuleType.ANDROID_BINARY.getBuckName())
                .defaultVisibility()
                .deps(deps)
                .name(bin(target))
                .extraBuckOpts(target.getExtraOpts(RuleType.ANDROID_BINARY))
    }
}
