package com.uber.okbuck.generator

import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.composer.android.AndroidBinaryRuleComposer
import com.uber.okbuck.composer.android.AndroidBuckRuleComposer
import com.uber.okbuck.composer.android.AndroidBuildConfigRuleComposer
import com.uber.okbuck.composer.android.AndroidInstrumentationApkRuleComposer
import com.uber.okbuck.composer.android.AndroidInstrumentationTestRuleComposer
import com.uber.okbuck.composer.android.AndroidLibraryRuleComposer
import com.uber.okbuck.composer.android.AndroidManifestRuleComposer
import com.uber.okbuck.composer.android.AndroidResourceRuleComposer
import com.uber.okbuck.composer.android.AndroidTestRuleComposer
import com.uber.okbuck.composer.android.ExopackageAndroidLibraryRuleComposer
import com.uber.okbuck.composer.android.GenAidlRuleComposer
import com.uber.okbuck.composer.android.KeystoreRuleComposer
import com.uber.okbuck.composer.android.LintRuleComposer
import com.uber.okbuck.composer.android.PreBuiltNativeLibraryRuleComposer
import com.uber.okbuck.composer.android.TrasformDependencyWriterRuleComposer
import com.uber.okbuck.composer.java.JavaLibraryRuleComposer
import com.uber.okbuck.composer.java.JavaTestRuleComposer
import com.uber.okbuck.config.BUCKFile
import com.uber.okbuck.core.io.FilePrinter
import com.uber.okbuck.core.io.Printer
import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.core.model.android.AndroidInstrumentationTarget
import com.uber.okbuck.core.model.android.AndroidLibTarget
import com.uber.okbuck.core.model.base.ProjectType
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.core.model.base.Target
import com.uber.okbuck.core.model.groovy.GroovyLibTarget
import com.uber.okbuck.core.model.java.JavaLibTarget
import com.uber.okbuck.core.model.kotlin.KotlinLibTarget
import com.uber.okbuck.core.model.scala.ScalaLibTarget
import com.uber.okbuck.core.util.ProjectUtil
import com.uber.okbuck.extension.LintExtension
import com.uber.okbuck.extension.OkBuckExtension
import com.uber.okbuck.rule.android.AndroidLibraryRule
import com.uber.okbuck.rule.android.AndroidManifestRule
import com.uber.okbuck.rule.android.AndroidResourceRule
import com.uber.okbuck.rule.android.ExopackageAndroidLibraryRule
import com.uber.okbuck.rule.android.GenAidlRule
import com.uber.okbuck.rule.base.BuckRule
import com.uber.okbuck.rule.base.GenRule
import org.gradle.api.Project

final class BuckFileGenerator {

    private BuckFileGenerator() {}

    /**
     * generate {@code BUCKFile}
     */
    static void generate(Project project) {
        List<BuckRule> rules = createRules(project)

        if (rules) {
            BUCKFile buckFile = new BUCKFile(rules)
            try {
                Printer printer = new FilePrinter(project.file(OkBuckGradlePlugin.BUCK))
                buckFile.print(printer)
                printer.flush()
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e)
            }
        }
    }

    private static List<BuckRule> createRules(Project project) {
        List<BuckRule> rules = []
        ProjectType projectType = ProjectUtil.getType(project)
        ProjectUtil.getTargets(project).each { String name, Target target ->
            switch (projectType) {
                case ProjectType.JAVA_LIB:
                    rules.addAll(createRules((JavaLibTarget) target))
                    break
                case ProjectType.GROOVY_LIB:
                    rules.addAll(createRules((GroovyLibTarget) target))
                    break
                case ProjectType.KOTLIN_LIB:
                    rules.addAll(createRules((KotlinLibTarget) target))
                    break
                case ProjectType.SCALA_LIB:
                    rules.addAll(createRules((ScalaLibTarget) target))
                    break
                case ProjectType.ANDROID_LIB:
                    rules.addAll(createRules((AndroidLibTarget) target))
                    break
                case ProjectType.ANDROID_APP:
                    AndroidAppTarget androidAppTarget = (AndroidAppTarget) target
                    List<BuckRule> targetRules = createRules(androidAppTarget)
                    rules.addAll(targetRules)
                    if (androidAppTarget.instrumentationTarget) {
                        rules.addAll(createRules(androidAppTarget.instrumentationTarget, androidAppTarget, targetRules))
                    }
                    break
                default:
                    throw new IllegalArgumentException("Okbuck does not support ${project} type projects yet. Please " +
                            "use the extension option okbuck.buckProjects to exclude ${project}.")
            }
        }

        // de-dup rules by name
        rules = rules.unique { rule ->
            rule.name
        }

        return rules
    }

    private static List<BuckRule> createRules(JavaLibTarget target) {
        List<BuckRule> rules = []
        rules.addAll(JavaLibraryRuleComposer.compose(target))

        if (target.test.sources) {
            rules.add(JavaTestRuleComposer.compose(target))
        }
        return rules
    }

    private static List<BuckRule> createRules(GroovyLibTarget target) {
        List<BuckRule> rules = []
        rules.addAll(JavaLibraryRuleComposer.compose(target, RuleType.GROOVY_LIBRARY))

        if (target.test.sources) {
            rules.add(JavaTestRuleComposer.compose(target, RuleType.GROOVY_TEST))
        }
        return rules
    }

    private static List<BuckRule> createRules(KotlinLibTarget target) {
        List<BuckRule> rules = []
        rules.addAll(JavaLibraryRuleComposer.compose(target, RuleType.KOTLIN_LIBRARY))

        if (target.test.sources) {
            rules.add(JavaTestRuleComposer.compose(target, RuleType.KOTLIN_TEST))
        }
        return rules
    }

    private static List<BuckRule> createRules(ScalaLibTarget target) {
        List<BuckRule> rules = []
        rules.addAll(JavaLibraryRuleComposer.compose(target, RuleType.SCALA_LIBRARY))

        if (target.test.sources) {
            rules.add(JavaTestRuleComposer.compose(target, RuleType.SCALA_TEST))
        }
        return rules
    }

    private static List<BuckRule> createRules(AndroidLibTarget target, String appClass = null,
                                              List<String> extraDeps = []) {
        List<BuckRule> rules = []
        List<BuckRule> androidLibRules = []

        // Aidl
        List<BuckRule> aidlRules = target.aidl.collect { String aidlDir ->
            GenAidlRuleComposer.compose(target, aidlDir)
        }
        List<String> aidlRuleNames = aidlRules.collect { GenAidlRule rule ->
            ":${rule.name}"
        }
        androidLibRules.addAll(aidlRules)

        // Res
        androidLibRules.add(AndroidResourceRuleComposer.compose(target))

        // BuildConfig
        androidLibRules.add(AndroidBuildConfigRuleComposer.compose(target))

        // Jni
        androidLibRules.addAll(target.jniLibs.collect { String jniLib ->
            PreBuiltNativeLibraryRuleComposer.compose(target, jniLib)
        })

        List<String> deps = androidLibRules.collect { BuckRule rule ->
            ":${rule.name}"
        } as List<String>
        deps.addAll(extraDeps)

        // Lib
        androidLibRules.add(AndroidLibraryRuleComposer.compose(
                target,
                deps,
                aidlRuleNames,
                appClass
        ))

        // Test
        if (target.robolectric && target.test.sources && !target.isTest) {
            androidLibRules.add(AndroidTestRuleComposer.compose(
                    target,
                    deps,
                    aidlRuleNames,
                    appClass))
        }

        OkBuckExtension okbuck = target.rootProject.okbuck
        LintExtension lint = okbuck.lint
        if (!lint.disabled) {
            androidLibRules.addAll(LintRuleComposer.compose(target))
        }

        rules.addAll(androidLibRules)
        return rules
    }

    private static List<BuckRule> createRules(AndroidAppTarget target) {
        List<BuckRule> rules = [] as List<BuckRule>
        List<String> deps = [":${AndroidBuckRuleComposer.src(target)}"]

        Set<BuckRule> libRules = createRules((AndroidLibTarget) target,
                target.exopackage ? target.exopackage.appClass : null) as Set<BuckRule>
        rules.addAll(libRules)

        libRules.each { BuckRule rule ->
            if (rule instanceof AndroidResourceRule && rule.name != null) {
                deps.add(":${rule.name}")
            }
        }

        AndroidManifestRule manifestRule = AndroidManifestRuleComposer.compose(target)
        rules.add(manifestRule)

        String keystoreRuleName = KeystoreRuleComposer.compose(target)

        if (target.exopackage) {
            ExopackageAndroidLibraryRule exoPackageRule =
                    ExopackageAndroidLibraryRuleComposer.compose(
                            target)
            rules.add(exoPackageRule)
            deps.add(":${exoPackageRule.name}")
        }

        List<GenRule> transformGenRules = TrasformDependencyWriterRuleComposer.compose(target)
        rules.addAll(transformGenRules)

        rules.add(AndroidBinaryRuleComposer.compose(
                target, deps, ":${manifestRule.name}", keystoreRuleName, transformGenRules))

        return rules
    }

    private static List<BuckRule> createRules(AndroidInstrumentationTarget target, AndroidAppTarget mainApkTarget,
                                              List<BuckRule> mainApkTargetRules) {
        List<BuckRule> rules = []

        Set<BuckRule> libRules = createRules((AndroidLibTarget) target, null, filterAndroidDepRules(mainApkTargetRules))
        rules.addAll(libRules)

        AndroidManifestRule manifestRule = AndroidManifestRuleComposer.compose(target, target.instrumentation)
        rules.add(manifestRule)

        rules.add(AndroidInstrumentationApkRuleComposer.compose(filterAndroidDepRules(rules), ":${manifestRule.name}", mainApkTarget))
        rules.add(AndroidInstrumentationTestRuleComposer.compose(mainApkTarget))
        return rules
    }

    private static List<String> filterAndroidDepRules(List<BuckRule> rules) {
        return rules.findAll { BuckRule rule ->
            rule instanceof AndroidLibraryRule || rule instanceof AndroidResourceRule
        }.collect {
            ":${it.name}"
        }
    }
}
