package com.uber.okbuck.composer.android;

import com.uber.okbuck.core.model.android.AndroidAppTarget;
import com.uber.okbuck.core.model.android.ExoPackageScope;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.core.util.D8Util;
import com.uber.okbuck.template.android.AndroidRule;
import com.uber.okbuck.template.core.Rule;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ExopackageAndroidLibraryRuleComposer extends AndroidBuckRuleComposer {

  private ExopackageAndroidLibraryRuleComposer() {
    // no instance
  }

  public static Rule compose(AndroidAppTarget target) {
    List<String> deps = new ArrayList<>();
    ExoPackageScope exopackage = target.getExopackage();

    Set<String> extraBuckOpts = new HashSet<>(target.getExtraOpts(RuleType.ANDROID_LIBRARY));
    if (exopackage != null) {
      deps.addAll(external(exopackage.getExternalDeps()));
      deps.addAll(targets(exopackage.getTargetDeps()));
      extraBuckOpts.add("srcs = ['" + exopackage.getAppClass() + "']");
    }
    deps.add(":" + buildConfig(target));

    Set<String> libraryAptDeps = new LinkedHashSet<>();
    libraryAptDeps.addAll(externalApt(target.getApt().getExternalJarDeps()));
    libraryAptDeps.addAll(targetsApt(target.getApt().getTargetDeps()));

    Set<String> providedDeps = new LinkedHashSet<>();
    providedDeps.add(D8Util.RT_STUB_JAR_RULE);

    AndroidRule androidRule =
        new AndroidRule()
            .sourceCompatibility(target.getSourceCompatibility())
            .targetCompatibility(target.getTargetCompatibility())
            .apPlugins(getApPlugins(target.getApPlugins()))
            .aptDeps(libraryAptDeps)
            .providedDeps(providedDeps)
            .options(mapOptions(target.getMain().getCompilerOptions()));

    if (target.getRuleType().equals(RuleType.KOTLIN_ANDROID_LIBRARY)) {
      androidRule = androidRule.language("kotlin");
    }

    return androidRule
        .ruleType(target.getRuleType().getBuckName())
        .defaultVisibility()
        .deps(deps)
        .name(appLib(target))
        .extraBuckOpts(extraBuckOpts);
  }
}
