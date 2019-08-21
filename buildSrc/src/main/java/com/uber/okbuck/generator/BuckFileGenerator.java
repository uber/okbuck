package com.uber.okbuck.generator;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.android.AndroidBinaryRuleComposer;
import com.uber.okbuck.composer.android.AndroidBuckRuleComposer;
import com.uber.okbuck.composer.android.AndroidBuildConfigRuleComposer;
import com.uber.okbuck.composer.android.AndroidInstrumentationApkRuleComposer;
import com.uber.okbuck.composer.android.AndroidInstrumentationTestRuleComposer;
import com.uber.okbuck.composer.android.AndroidTestRuleComposer;
import com.uber.okbuck.composer.android.ExopackageAndroidLibraryRuleComposer;
import com.uber.okbuck.composer.android.GenAidlRuleComposer;
import com.uber.okbuck.composer.android.KeystoreRuleComposer;
import com.uber.okbuck.composer.android.ManifestRuleComposer;
import com.uber.okbuck.composer.android.PreBuiltNativeLibraryRuleComposer;
import com.uber.okbuck.composer.android.AndroidModuleRuleComposer;
import com.uber.okbuck.composer.jvm.JvmIntegrationTestRuleComposer;
import com.uber.okbuck.composer.jvm.JvmLibraryRuleComposer;
import com.uber.okbuck.composer.jvm.JvmTestRuleComposer;
import com.uber.okbuck.core.manager.BuckFileManager;
import com.uber.okbuck.core.model.android.AndroidAppInstrumentationTarget;
import com.uber.okbuck.core.model.android.AndroidAppTarget;
import com.uber.okbuck.core.model.android.AndroidLibInstrumentationTarget;
import com.uber.okbuck.core.model.android.AndroidLibTarget;
import com.uber.okbuck.core.model.base.ProjectType;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.core.model.jvm.JvmTarget;
import com.uber.okbuck.core.util.ProjectCache;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.TestExtension;
import com.uber.okbuck.extension.VisibilityExtension;
import com.uber.okbuck.template.android.AndroidModuleRule;
import com.uber.okbuck.template.android.AndroidRule;
import com.uber.okbuck.template.core.Rule;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.gradle.api.Project;

public final class BuckFileGenerator {

  private BuckFileGenerator() {}

  /** generate {@code BUCKFile} */
  public static void generate(
      Project project,
      BuckFileManager buckFileManager,
      VisibilityExtension visibilityExtension,
      TestExtension testExtension) {
    File moduleDir = project.getBuildFile().getParentFile();
    File visibilityFile = new File(moduleDir, visibilityExtension.visibilityFileName);
    boolean hasVisibilityFile = visibilityFile.isFile();
    boolean hasIntegrationTests = testExtension.enableIntegrationTests;

    List<Rule> rules = createRules(project, hasIntegrationTests);

    Multimap<String, String> extraLoadStatements = TreeMultimap.create();

    if (hasVisibilityFile) {
      rules.forEach(rule -> rule.fileConfiguredVisibility(true));
      extraLoadStatements.put(
          ":" + visibilityExtension.visibilityFileName, visibilityExtension.visibilityFunction);
    }

    File buckFile = project.file(OkBuckGradlePlugin.BUCK);
    buckFileManager.writeToBuckFile(rules, buckFile, extraLoadStatements);
  }

  private static List<Rule> createRules(Project project, boolean hasIntegrationTests) {
    List<Rule> rules = new ArrayList<>();
    ProjectType projectType = ProjectUtil.getType(project);

    ProjectCache.getTargetCache(project)
        .getTargets()
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
                          projectType.getTestRuleType(),
                          hasIntegrationTests));
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
      JvmTarget target,
      RuleType mainRuleType,
      RuleType testRuleType,
      boolean hasIntegrationTests) {
    List<Rule> rules = new ArrayList<>(JvmLibraryRuleComposer.compose(target, mainRuleType));

    if (!target.getTest().getSources().isEmpty()) {
      rules.add(JvmTestRuleComposer.compose(target, testRuleType));
    }
    if (hasIntegrationTests && !target.getIntegrationTest().getSources().isEmpty()) {
      rules.add(JvmIntegrationTestRuleComposer.compose(target, testRuleType));
    }

    return rules;
  }

  private static List<Rule> createRules(
      AndroidLibTarget target,
      @Nullable String appClass,
      List<String> extraDeps,
      List<String> extraResDeps) {

    String manifestRuleName = ":" +  AndroidBuckRuleComposer.libManifest(target);
    List<Rule> androidLibRules = new ArrayList<>();

    // Aidl
    List<Rule> aidlRules =
        target
            .getAidl()
            .stream()
            .map(aidlDir -> GenAidlRuleComposer.compose(target, aidlDir, manifestRuleName))
            .collect(Collectors.toList());

    List<String> aidlRuleNames =
        aidlRules.stream().map(Rule::buckName).collect(Collectors.toList());

    androidLibRules.addAll(aidlRules);

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

    List<String> deps = androidLibRules.stream().map(Rule::buckName).collect(Collectors.toList());
    deps.addAll(extraDeps);


    // Unified android lib
    androidLibRules.add(
        AndroidModuleRuleComposer.compose(
            target, deps, aidlRuleNames, appClass, extraResDeps));

    // Test
    if (target.getRobolectricEnabled()
        && !target.getTest().getSources().isEmpty()
        && !target.getIsTest()) {
      androidLibRules.add(
          AndroidTestRuleComposer.compose(
              target, manifestRuleName, deps, aidlRuleNames, appClass));
    }

    return new ArrayList<>(androidLibRules);
  }

  private static List<Rule> createRules(AndroidLibTarget target) {
    return createRules(target, null, new ArrayList<>(), new ArrayList<>());
  }

  private static List<Rule> createRules(
      AndroidAppTarget target, List<String> additionalDeps, List<String> additionalResDeps) {
    List<String> deps = new ArrayList<>();
    deps.add(":" + AndroidBuckRuleComposer.src(target));

    deps.addAll(additionalDeps);

    List<Rule> libRules =
        createRules(
            target,
            target.getExopackage() != null ? target.getExopackage().getAppClass() : null,
            additionalDeps,
            additionalResDeps);
    List<Rule> rules = new ArrayList<>(libRules);

    libRules.forEach(
        rule -> {
          if (rule instanceof AndroidModuleRule && rule.name() != null) {
            deps.add(rule.buckName().replace(":src_", ":res_"));
          }
        });

    Rule keystoreRule = KeystoreRuleComposer.compose(target);

    if (target.getExopackage() != null) {
      Rule exoPackageRule = ExopackageAndroidLibraryRuleComposer.compose(target);
      rules.add(exoPackageRule);
      deps.add(exoPackageRule.buckName());
    }

    if (keystoreRule != null) {
      rules.add(keystoreRule);

      Rule appManifest = ManifestRuleComposer.composeForBinary(target);
      rules.add(appManifest);

      rules.add(
          AndroidBinaryRuleComposer.compose(
              target,
              appManifest.buckName(),
              deps,
              ":" + AndroidBuckRuleComposer.keystore(target)));
    }

    return rules;
  }

  private static List<Rule> createRules(AndroidAppTarget target) {
    return createRules(target, new ArrayList<>(), new ArrayList<>());
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

    Rule testAppManifest = ManifestRuleComposer.composeForBinary(target);
    rules.add(testAppManifest);

    rules.add(
        AndroidInstrumentationApkRuleComposer.compose(
            filterAndroidDepRules(rules), mainApkTarget, testAppManifest.buckName()));
    rules.add(AndroidInstrumentationTestRuleComposer.compose(mainApkTarget));
    return rules;
  }

  private static List<Rule> createRules(
      AndroidLibInstrumentationTarget target, List<Rule> mainLibTargetRules) {
    return new ArrayList<>(
        createRules(
            target,
            filterAndroidDepRules(mainLibTargetRules),
            filterAndroidResDepRules(mainLibTargetRules)));
  }

  // android rules now accept only other android rules as deps, and okbuck_android_module
  // macro infers what resources rules to depend on
  private static List<String> filterAndroidDepRules(List<Rule> rules) {
    return rules
        .stream()
        .filter(rule -> rule instanceof AndroidRule || rule instanceof AndroidModuleRule)
        .map(Rule::buckName)
        .collect(Collectors.toList());
  }

  // Same logic as above, so to get the actual resource rule, we derive from the src one
  private static List<String> filterAndroidResDepRules(List<Rule> rules) {
    return rules
        .stream()
        .filter(rule -> rule instanceof AndroidRule || rule instanceof AndroidModuleRule)
        .map(Rule::buckName)
        .map(ruleName -> ruleName.replace(":src_", ":res_"))
        .collect(Collectors.toList());
  }
}
