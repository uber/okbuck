package com.uber.okbuck.composer.android;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.uber.okbuck.core.manager.TransformManager;
import com.uber.okbuck.core.model.android.AndroidAppTarget;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.template.android.AndroidBinaryRule;
import com.uber.okbuck.template.core.Rule;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

public final class AndroidBinaryRuleComposer extends AndroidBuckRuleComposer {

  private static final ImmutableMap<String, String> CPU_FILTER_MAP =
      new ImmutableMap.Builder<String, String>()
          .put("armeabi", "ARM")
          .put("armeabi-v7a", "ARMV7")
          .put("arm64-v8a", "ARM64")
          .put("x86", "X86")
          .put("x86_64", "X86_64")
          .put("mips", "MIPS")
          .build();

  private AndroidBinaryRuleComposer() {
    // no instance
  }

  public static Rule compose(
      AndroidAppTarget target, String manifestRule, List<String> deps, String keystoreRuleName) {
    Set<String> mappedCpuFilters =
        target
            .getCpuFilters()
            .stream()
            .map(CPU_FILTER_MAP::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    TransformManager transformManager = ProjectUtil.getTransformManager(target.getProject());
    Pair<String, List<String>> results = transformManager.getBashCommandAndTransformDeps(target);
    String bashCommand = results.getLeft();
    List<String> transformDeps = results.getRight();
    transformDeps.add(TransformManager.TRANSFORM_RULE);

    List<String> testTargets =
        target.getAppInstrumentationTarget() != null
            ? ImmutableList.of(":" + instrumentationTest(target))
            : ImmutableList.of();

    String proguardConfig = target.getProguardConfig();
    if (proguardConfig != null) {
      ProjectUtil.getPlugin(target.getRootProject()).exportedPaths.add(proguardConfig);
      String proguardMapping = target.getProguardMapping();
      if (proguardMapping != null) {
        ProjectUtil.getPlugin(target.getRootProject()).exportedPaths.add(proguardMapping);
        deps.add(fileRule(proguardMapping));
      }
    }

    if (proguardConfig != null && target.getProguardMapping() != null) {
      deps.add(fileRule(target.getProguardMapping()));
    }

    return new AndroidBinaryRule()
        .manifestSkeleton(manifestRule)
        .keystore(keystoreRuleName)
        .multidexEnabled(target.getMultidexEnabled())
        .linearAllocHardLimit(target.getLinearAllocHardLimit())
        .primaryDexPatterns(target.getPrimaryDexPatterns())
        .exopackage(target.getExopackage() != null)
        .cpuFilters(mappedCpuFilters)
        .minifyEnabled(target.getMinifyEnabled())
        .proguardConfig(fileRule(proguardConfig))
        .debuggable(target.getDebuggable())
        .placeholders(target.getPlaceholders())
        .includesVectorDrawables(target.getIncludesVectorDrawables())
        .preprocessJavaClassesDeps(transformDeps)
        .preprocessJavaClassesBash(bashCommand)
        .testTargets(testTargets)
        .ruleType(RuleType.ANDROID_BINARY.getBuckName())
        .defaultVisibility()
        .deps(deps)
        .name(bin(target))
        .extraBuckOpts(target.getExtraOpts(RuleType.ANDROID_BINARY));
  }
}
