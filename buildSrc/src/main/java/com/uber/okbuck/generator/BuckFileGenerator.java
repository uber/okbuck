package com.uber.okbuck.generator;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.android.AndroidBinaryRuleComposer;
import com.uber.okbuck.composer.android.AndroidBuckRuleComposer;
import com.uber.okbuck.composer.android.AndroidBuildConfigRuleComposer;
import com.uber.okbuck.composer.android.AndroidInstrumentationApkRuleComposer;
import com.uber.okbuck.composer.android.AndroidInstrumentationTestRuleComposer;
import com.uber.okbuck.composer.android.AndroidLibraryRuleComposer;
import com.uber.okbuck.composer.android.AndroidResourceRuleComposer;
import com.uber.okbuck.composer.android.AndroidTestRuleComposer;
import com.uber.okbuck.composer.android.ExopackageAndroidLibraryRuleComposer;
import com.uber.okbuck.composer.android.GenAidlRuleComposer;
import com.uber.okbuck.composer.android.KeystoreRuleComposer;
import com.uber.okbuck.composer.android.LintRuleComposer;
import com.uber.okbuck.composer.android.PreBuiltNativeLibraryRuleComposer;
import com.uber.okbuck.composer.jvm.JvmLibraryRuleComposer;
import com.uber.okbuck.composer.jvm.JvmTestRuleComposer;
import com.uber.okbuck.core.model.android.AndroidAppInstrumentationTarget;
import com.uber.okbuck.core.model.android.AndroidAppTarget;
import com.uber.okbuck.core.model.android.AndroidLibInstrumentationTarget;
import com.uber.okbuck.core.model.android.AndroidLibTarget;
import com.uber.okbuck.core.model.base.ProjectType;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.core.model.jvm.JvmTarget;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.template.android.AndroidRule;
import com.uber.okbuck.template.android.ResourceRule;
import com.uber.okbuck.template.core.Rule;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.gradle.api.Project;

public final class BuckFileGenerator {

  /** generate {@code BUCKFile} */
  public static void generate(Project project) {
    List<Rule> rules = createRules(project);
    File buckFile = project.file(OkBuckGradlePlugin.BUCK);
    FileUtil.writeToBuckFile(rules, buckFile);
  }

  private static List<Rule> createRules(Project project) {
    final List<Rule> rules = new ArrayList<>();
    ProjectType projectType = ProjectUtil.getType(project);
    ProjectUtil.getTargets(project)
        .forEach(
            (name, target) -> {
              switch (projectType) {
                case JAVA_LIB:
                case GROOVY_LIB:
                case KOTLIN_LIB:
                case SCALA_LIB:
                  rules.addAll(
                      createRules(
                          (JvmTarget) target,
                          projectType.getMainRuleType(),
                          projectType.getTestRuleType()));
                  break;
                case ANDROID_LIB:
                  AndroidLibTarget androidLibTarget = (AndroidLibTarget) target;
                  List<Rule> targetRules = createRules(androidLibTarget);
                  rules.addAll(targetRules);
                  if (androidLibTarget.getLibInstrumentationTarget() != null) {
                    rules.addAll(
                        createRules(androidLibTarget.getLibInstrumentationTarget(), targetRules));
                  }
                  break;
                case ANDROID_APP:
                  AndroidAppTarget androidAppTarget = (AndroidAppTarget) target;
                  targetRules = createRules(androidAppTarget);
                  rules.addAll(targetRules);
                  if (androidAppTarget.getAppInstrumentationTarget() != null) {
                    rules.addAll(
                        createRules(
                            androidAppTarget.getAppInstrumentationTarget(),
                            androidAppTarget,
                            targetRules));
                  }
                  break;
                default:
                  throw new IllegalArgumentException(
                      "Okbuck does not support "
                          + project
                          + "type projects yet. Please use the extension option okbuck.buckProjects to exclude "
                          + project);
              }
            });

    // de-dup rules by name
    return new ArrayList<>(new LinkedHashSet<>(rules));
  }

  private static List<Rule> createRules(
      JvmTarget target, RuleType mainRuleType, RuleType testRuleType) {
    List<Rule> rules = new ArrayList<>(JvmLibraryRuleComposer.compose(target, mainRuleType));

    if (!target.getTest().getSources().isEmpty()) {
      rules.add(JvmTestRuleComposer.compose(target, testRuleType));
    }

    return rules;
  }

  private static List<Rule> createRules(
      AndroidLibTarget target,
      @Nullable String appClass,
      List<String> extraDeps,
      List<String> extraResDeps) {

    // Aidl
    List<Rule> aidlRules =
        target
            .getAidl()
            .stream()
            .map(aidlDir -> GenAidlRuleComposer.compose(target, aidlDir))
            .collect(Collectors.toList());

    List<String> aidlRuleNames =
        aidlRules.stream().map(rule -> ":" + rule.name()).collect(Collectors.toList());

    List<Rule> androidLibRules = new ArrayList<>(aidlRules);

    // Res
    androidLibRules.add(AndroidResourceRuleComposer.compose(target, extraResDeps));

    // BuildConfig
    if (target.shouldGenerateBuildConfig()) {
      androidLibRules.add(AndroidBuildConfigRuleComposer.compose(target));
    }

    // Jni
    androidLibRules.addAll(
        target
            .getJniLibs()
            .stream()
            .map(jniLib -> PreBuiltNativeLibraryRuleComposer.compose(target, jniLib))
            .collect(Collectors.toList()));

    List<String> deps =
        androidLibRules.stream().map(rule -> ":" + rule.name()).collect(Collectors.toList());
    deps.addAll(extraDeps);

    // Lib
    androidLibRules.add(AndroidLibraryRuleComposer.compose(target, deps, aidlRuleNames, appClass));

    // Test
    if (target.getRobolectricEnabled()
        && !target.getTest().getSources().isEmpty()
        && !target.getIsTest()) {
      androidLibRules.add(AndroidTestRuleComposer.compose(target, deps, aidlRuleNames, appClass));
    }

    // Lint
    if (target.getLintEnabled() && !target.getIsTest()) {
      androidLibRules.add(LintRuleComposer.compose(target));
    }

    return new ArrayList<>(androidLibRules);
  }

  private static List<Rule> createRules(AndroidLibTarget target, @Nullable String appClass) {
    return BuckFileGenerator.createRules(target, appClass, new ArrayList<>(), new ArrayList<>());
  }

  private static List<Rule> createRules(AndroidLibTarget target) {
    return BuckFileGenerator.createRules(target, null, new ArrayList<>(), new ArrayList<>());
  }

  private static List<Rule> createRules(AndroidAppTarget target, List<String> additionalDeps) {
    List<String> deps = new ArrayList<>();
    deps.add(":" + AndroidBuckRuleComposer.src(target));

    deps.addAll(additionalDeps);

    List<Rule> libRules =
        createRules(
            target, target.getExopackage() != null ? target.getExopackage().getAppClass() : null);
    List<Rule> rules = new ArrayList<>(libRules);

    libRules.forEach(
        rule -> {
          if (rule instanceof ResourceRule && rule.name() != null) {
            deps.add(":" + rule.name());
          }
        });

    String keystoreRuleName = KeystoreRuleComposer.compose(target);

    if (target.getExopackage() != null) {
      Rule exoPackageRule = ExopackageAndroidLibraryRuleComposer.compose(target);
      rules.add(exoPackageRule);
      deps.add(":" + exoPackageRule.name());
    }

    rules.add(AndroidBinaryRuleComposer.compose(target, deps, keystoreRuleName));

    return rules;
  }

  private static List<Rule> createRules(AndroidAppTarget target) {
    return BuckFileGenerator.createRules(target, new ArrayList<>());
  }

  private static List<Rule> createRules(
      AndroidAppInstrumentationTarget target,
      AndroidAppTarget mainApkTarget,
      List<Rule> mainApkTargetRules) {

    List<Rule> libRules =
        createRules(
            target,
            null,
            filterAndroidDepRules(mainApkTargetRules),
            filterAndroidResDepRules(mainApkTargetRules));
    List<Rule> rules = new ArrayList<>(libRules);

    rules.add(
        AndroidInstrumentationApkRuleComposer.compose(
            filterAndroidDepRules(rules), target, mainApkTarget));
    rules.add(AndroidInstrumentationTestRuleComposer.compose(mainApkTarget));
    return rules;
  }

  private static List<Rule> createRules(
      AndroidLibInstrumentationTarget target, List<Rule> mainLibTargetRules) {

    List<Rule> rules = new ArrayList<>();

    List<Rule> libRules =
        createRules(
            target,
            null,
            filterAndroidDepRules(mainLibTargetRules),
            filterAndroidResDepRules(mainLibTargetRules));

    rules.addAll(libRules);
    rules.addAll(createRules(target, filterAndroidDepRules(rules)));
    return rules;
  }

  private static List<String> filterAndroidDepRules(List<Rule> rules) {
    return rules
        .stream()
        .filter(rule -> rule instanceof AndroidRule || rule instanceof ResourceRule)
        .map(rule -> ":" + rule.name())
        .collect(Collectors.toList());
  }

  private static List<String> filterAndroidResDepRules(List<Rule> rules) {
    return rules
        .stream()
        .filter(rule -> rule instanceof ResourceRule)
        .map(rule -> ":" + rule.name())
        .collect(Collectors.toList());
  }
}
