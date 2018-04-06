package com.uber.okbuck.generator

import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.composer.android.AndroidBinaryRuleComposer
import com.uber.okbuck.composer.android.AndroidBuckRuleComposer
import com.uber.okbuck.composer.android.AndroidBuildConfigRuleComposer
import com.uber.okbuck.composer.android.AndroidInstrumentationApkRuleComposer
import com.uber.okbuck.composer.android.AndroidInstrumentationTestRuleComposer
import com.uber.okbuck.composer.android.AndroidLibraryRuleComposer
import com.uber.okbuck.composer.android.AndroidResourceRuleComposer
import com.uber.okbuck.composer.android.AndroidTestRuleComposer
import com.uber.okbuck.composer.android.ExopackageAndroidLibraryRuleComposer
import com.uber.okbuck.composer.android.GenAidlRuleComposer
import com.uber.okbuck.composer.android.KeystoreRuleComposer
import com.uber.okbuck.composer.android.LintRuleComposer
import com.uber.okbuck.composer.android.PreBuiltNativeLibraryRuleComposer
import com.uber.okbuck.composer.jvm.JvmLibraryRuleComposer
import com.uber.okbuck.composer.jvm.JvmTestRuleComposer
import com.uber.okbuck.core.model.android.AndroidAppInstrumentationTarget
import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.core.model.android.AndroidLibInstrumentationTarget
import com.uber.okbuck.core.model.android.AndroidLibTarget
import com.uber.okbuck.core.model.base.ProjectType
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.core.model.base.Target
import com.uber.okbuck.core.model.jvm.JvmTarget
import com.uber.okbuck.core.util.ProjectUtil
import com.uber.okbuck.template.android.AndroidRule
import com.uber.okbuck.template.android.ResourceRule
import com.uber.okbuck.template.core.Rule
import org.gradle.api.Project

final class BuckFileGenerator {

    private static final byte[] NEWLINE = System.lineSeparator().getBytes()

    private BuckFileGenerator() {}

    /**
     * generate {@code BUCKFile}
     */
    static void generate(Project project) {
        List<Rule> rules = createRules(project)

        if (rules) {
            final OutputStream os = new FileOutputStream(project.file(OkBuckGradlePlugin.BUCK))
            for (Rule rule : rules) {
                rule.render(os)
                os.write(NEWLINE)
            }
            os.flush()
            os.close()
        }
    }

    private static List<Rule> createRules(Project project) {
        List<Rule> rules = []
        ProjectType projectType = ProjectUtil.getType(project)
        ProjectUtil.getTargets(project).each { String name, Target target ->
            switch (projectType) {
                case ProjectType.JAVA_LIB:
                case ProjectType.GROOVY_LIB:
                case ProjectType.KOTLIN_LIB:
                case ProjectType.SCALA_LIB:
                    rules.addAll(createRules((JvmTarget) target, projectType.mainRuleType, projectType.testRuleType))
                    break
                case ProjectType.ANDROID_LIB:
                    AndroidLibTarget androidLibTarget = (AndroidLibTarget) target
                    List<Rule> targetRules = createRules(androidLibTarget)
                    rules.addAll(targetRules)
                    if (androidLibTarget.libInstrumentationTarget) {
                        rules.addAll(createRules(androidLibTarget.libInstrumentationTarget, targetRules))
                    }
                    break
                case ProjectType.ANDROID_APP:
                    AndroidAppTarget androidAppTarget = (AndroidAppTarget) target
                    List<Rule> targetRules = createRules(androidAppTarget)
                    rules.addAll(targetRules)
                    if (androidAppTarget.appInstrumentationTarget) {
                        rules.addAll(createRules(androidAppTarget.appInstrumentationTarget, androidAppTarget, targetRules))
                    }
                    break
                default:
                    throw new IllegalArgumentException("Okbuck does not support ${project} type projects yet. Please " +
                            "use the extension option okbuck.buckProjects to exclude ${project}.")
            }
        }

        // de-dup rules by name
        rules = rules.unique { rule ->
            rule.name()
        }

        return rules
    }

    private static List<Rule> createRules(JvmTarget target, RuleType mainRuleType, RuleType testRuleType) {
        List<Rule> rules = []
        rules.addAll(JvmLibraryRuleComposer.compose(target, mainRuleType))

        if (target.test.sources) {
            rules.add(JvmTestRuleComposer.compose(target, testRuleType))
        }
        return rules
    }

    private static List<Rule> createRules(AndroidLibTarget target, String appClass = null,
                                          List<String> extraDeps = []) {
        List<Rule> rules = []
        List<Rule> androidLibRules = []

        // Aidl
        List<Rule> aidlRules = target.aidl.collect { String aidlDir ->
            GenAidlRuleComposer.compose(target, aidlDir)
        }
        List<String> aidlRuleNames = aidlRules.collect { Rule rule ->
            ":${rule.name()}"
        }
        androidLibRules.addAll(aidlRules)

        // Res
        androidLibRules.add(AndroidResourceRuleComposer.compose(target))

        // BuildConfig
        if (target.shouldGenerateBuildConfig()) {
            androidLibRules.add(AndroidBuildConfigRuleComposer.compose(target))
        }

        // Jni
        androidLibRules.addAll(target.jniLibs.collect { String jniLib ->
            PreBuiltNativeLibraryRuleComposer.compose(target, jniLib)
        })

        List<String> deps = androidLibRules.collect { Rule rule ->
            ":${rule.name()}"
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
        if (target.robolectricEnabled && target.test.sources && !target.isTest) {
            androidLibRules.add(AndroidTestRuleComposer.compose(
                    target,
                    deps,
                    aidlRuleNames,
                    appClass))
        }

        // Lint
        if (target.lintEnabled && !target.isTest) {
            androidLibRules.add(LintRuleComposer.compose(target))
        }

        rules.addAll(androidLibRules)
        return rules
    }

    private static List<Rule> createRules(AndroidAppTarget target,
                                          List<String> additionalDeps = []) {
        List<Rule> rules = []
        List<String> deps = [":${AndroidBuckRuleComposer.src(target)}"]
        deps.addAll(additionalDeps)

        Set<Rule> libRules = createRules((AndroidLibTarget) target,
                target.exopackage ? target.exopackage.appClass : null)
        rules.addAll(libRules)

        libRules.each { Rule rule ->
            if (rule instanceof ResourceRule && rule.name() != null) {
                deps.add(":${rule.name()}")
            }
        }

        String keystoreRuleName = KeystoreRuleComposer.compose(target)

        if (target.exopackage) {
            Rule exoPackageRule = ExopackageAndroidLibraryRuleComposer.compose(target)
            rules.add(exoPackageRule)
            deps.add(":${exoPackageRule.name()}")
        }

        rules.add(AndroidBinaryRuleComposer.compose(target, deps, keystoreRuleName))

        return rules
    }

    private static List<Rule> createRules(AndroidAppInstrumentationTarget target, AndroidAppTarget mainApkTarget,
                                          List<Rule> mainApkTargetRules) {
        List<Rule> rules = []

        Set<Rule> libRules = createRules((AndroidLibTarget) target, null, filterAndroidDepRules(mainApkTargetRules))
        rules.addAll(libRules)

        rules.add(AndroidInstrumentationApkRuleComposer.compose(filterAndroidDepRules(rules), target, mainApkTarget))
        rules.add(AndroidInstrumentationTestRuleComposer.compose(mainApkTarget))
        return rules
    }

    private static List<Rule> createRules(AndroidLibInstrumentationTarget target, List<Rule> mainLibTargetRules) {
        List<Rule> rules = []

        // TODO: We should find a way to filter robolectric rule too, but Rules have no getter to find out its type, and robolectric rule at the end is a normal AndroidRule
        // TODO: res_<type>_test should also depend on res_<type>, haven't thought of a way to achieve that
        Set<Rule> libRules = createRules((AndroidLibTarget) target, null, filterAndroidDepRules(mainLibTargetRules))
        rules.addAll(libRules)

        rules.addAll(createRules((AndroidAppTarget) target, filterAndroidDepRules(rules)))
        return rules
    }

    private static List<String> filterAndroidDepRules(List<Rule> rules) {
        return rules.findAll { Rule rule ->
            rule instanceof AndroidRule || rule instanceof ResourceRule
        }.collect {
            ":${it.name()}"
        }
    }
}
