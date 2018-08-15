package com.uber.okbuck.composer.android;

import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.core.model.android.AndroidLibTarget;
import com.uber.okbuck.core.model.android.AndroidTarget;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.core.util.D8Util;
import com.uber.okbuck.template.android.AndroidRule;
import com.uber.okbuck.template.core.Rule;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public final class AndroidLibraryRuleComposer extends AndroidBuckRuleComposer {

  private AndroidLibraryRuleComposer() {
    // no instance
  }

  public static Rule compose(
      AndroidLibTarget target,
      List<String> deps,
      final List<String> aidlRuleNames,
      @Nullable String appClass) {

    Set<String> libraryDeps = new HashSet<>(deps);
    libraryDeps.addAll(external(getExternalDeps(target.getMain(), target.getProvided())));
    libraryDeps.addAll(targets(getTargetDeps(target.getMain(), target.getProvided())));

    List<String> libraryAptDeps = new ArrayList<>();
    libraryAptDeps.addAll(externalApt(target.getApt().getExternalJarDeps()));
    libraryAptDeps.addAll(targetsApt(target.getApt().getTargetDeps()));

    Set<String> providedDeps = new HashSet<>();
    providedDeps.addAll(external(getExternalProvidedDeps(target.getMain(), target.getProvided())));
    providedDeps.addAll(targets(getTargetProvidedDeps(target.getMain(), target.getProvided())));
    providedDeps.add(D8Util.RT_STUB_JAR_RULE);

    libraryDeps.addAll(
        getTargetDeps(target.getMain(), target.getProvided())
            .stream()
            .filter(targetDep -> targetDep instanceof AndroidTarget)
            .map(targetDep -> resRule((AndroidTarget) targetDep))
            .collect(Collectors.toSet()));

    List<String> testTargets = new ArrayList<>();
    if (target.getRobolectricEnabled() && !target.getTest().getSources().isEmpty()) {
      testTargets.add(":" + test(target));
    }

    if (target.getLibInstrumentationTarget() != null
        && !target.getLibInstrumentationTarget().getMain().getSources().isEmpty()) {
      testTargets.add(":" + AndroidBuckRuleComposer.bin(target.getLibInstrumentationTarget()));
    }

    AndroidRule androidRule =
        new AndroidRule()
            .srcs(target.getMain().getSources())
            .exts(target.getRuleType().getSourceExtensions())
            .manifest(fileRule(target.getManifest()))
            .proguardConfig(target.getConsumerProguardConfig())
            .apPlugins(getApPlugins(target.getApPlugins()))
            .aptDeps(libraryAptDeps)
            .providedDeps(providedDeps)
            .resources(target.getMain().getJavaResources())
            .sourceCompatibility(target.getSourceCompatibility())
            .targetCompatibility(target.getTargetCompatibility())
            .testTargets(testTargets)
            .exportedDeps(aidlRuleNames)
            .excludes(appClass != null ? ImmutableSet.of(appClass) : ImmutableSet.of())
            .generateR2(target.getGenerateR2())
            .options(mapOptions(target.getMain().getCompilerOptions()));

    if (target.getRuleType().equals(RuleType.KOTLIN_ANDROID_LIBRARY)) {
      androidRule = androidRule.language("kotlin");
    }

    return androidRule
        .ruleType(target.getRuleType().getBuckName())
        .defaultVisibility()
        .deps(libraryDeps)
        .name(src(target))
        .extraBuckOpts(target.getExtraOpts(RuleType.ANDROID_LIBRARY));
  }
}
