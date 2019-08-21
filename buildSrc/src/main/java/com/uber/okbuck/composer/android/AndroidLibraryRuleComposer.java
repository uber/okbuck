package com.uber.okbuck.composer.android;

import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.model.android.AndroidLibTarget;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.core.model.base.SourceSetType;
import com.uber.okbuck.core.model.jvm.JvmTarget;
import com.uber.okbuck.core.util.D8Util;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
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
      String manifestRule,
      List<String> deps,
      List<String> aidlRuleNames,
      @Nullable String appClass) {

    Set<String> libraryDeps = new HashSet<>(deps);
    libraryDeps.addAll(external(target.getExternalDeps(SourceSetType.MAIN)));
    libraryDeps.addAll(targets(target.getTargetDeps(SourceSetType.MAIN)));
    libraryDeps.addAll(resources(target.getTargetDeps(SourceSetType.MAIN)));
    libraryDeps.addAll(resources(target.getTargetExportedDeps(SourceSetType.MAIN)));

    List<String> libraryAptDeps = new ArrayList<>();
    libraryAptDeps.addAll(externalApt(target.getExternalAptDeps(SourceSetType.MAIN)));
    libraryAptDeps.addAll(targetsApt(target.getTargetAptDeps(SourceSetType.MAIN)));

    Set<String> providedDeps = new HashSet<>();
    providedDeps.addAll(external(target.getExternalProvidedDeps(SourceSetType.MAIN)));
    providedDeps.addAll(targets(target.getTargetProvidedDeps(SourceSetType.MAIN)));
    providedDeps.add(D8Util.RT_STUB_JAR_RULE);

    Set<String> libraryExportedDeps = new HashSet<>();
    libraryExportedDeps.addAll(external(target.getExternalExportedDeps(SourceSetType.MAIN)));
    libraryExportedDeps.addAll(targets(target.getTargetExportedDeps(SourceSetType.MAIN)));
    libraryExportedDeps.addAll(aidlRuleNames);

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
            .exts(target.getRuleType().getProperties())
            .manifest(manifestRule)
            .proguardConfig(target.getConsumerProguardConfig())
            .apPlugins(getApPlugins(target.getApPlugins()))
            .aptDeps(libraryAptDeps)
            .providedDeps(providedDeps)
            .exportedDeps(libraryExportedDeps)
            .resources(target.getMain().getJavaResources())
            .resDirs(target.getResDirs())
            .sourceCompatibility(target.getSourceCompatibility())
            .targetCompatibility(target.getTargetCompatibility())
            .testTargets(testTargets)
            .excludes(appClass != null ? ImmutableSet.of(appClass) : ImmutableSet.of())
            .generateR2(target.getGenerateR2())
            .options(target.getMain().getCustomOptions());

    if (target.getRuleType().equals(RuleType.KOTLIN_ANDROID_LIBRARY)) {
      androidRule.language("kotlin");
    }

    if (target.getLintEnabled()) {
      String lintConfigPath;
      if (target.getLintOptions() != null
          && target.getLintOptions().getLintConfig() != null
          && target.getLintOptions().getLintConfig().exists()) {
        lintConfigPath =
            FileUtil.getRelativePath(
                target.getRootProject().getProjectDir(), target.getLintOptions().getLintConfig());
        ProjectUtil.getPlugin(target.getRootProject()).exportedPaths.add(lintConfigPath);
      } else {
        lintConfigPath = null;
      }

      Set<String> customLintTargets =
          target
              .getLint()
              .getTargetDeps()
              .stream()
              .filter(t -> (t instanceof JvmTarget))
              .map(BuckRuleComposer::binTargets)
              .collect(Collectors.toSet());

      if (lintConfigPath != null) {
        androidRule
          .lintConfigXml(fileRule(lintConfigPath));
      }

      androidRule
          .customLints(customLintTargets)
          .lintOptions(target.getLintOptions());
    } else {
      androidRule.disableLint(true);
    }

    return androidRule
        .ruleType(target.getRuleType().getBuckName())
        .defaultVisibility()
        .deps(libraryDeps)
        .name(src(target))
        .extraBuckOpts(target.getExtraOpts(RuleType.ANDROID_LIBRARY));
  }
}
