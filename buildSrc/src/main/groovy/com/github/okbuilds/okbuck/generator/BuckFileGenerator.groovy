package com.github.okbuilds.okbuck.generator

import com.github.okbuilds.core.model.*
import com.github.okbuilds.core.util.ProjectUtil
import com.github.okbuilds.okbuck.ExperimentalExtension
import com.github.okbuilds.okbuck.OkBuckExtension
import com.github.okbuilds.okbuck.composer.*
import com.github.okbuilds.okbuck.config.BUCKFile
import com.github.okbuilds.okbuck.rule.*
import org.gradle.api.Project

final class BuckFileGenerator {

    private final Project mRootProject
    static OkBuckExtension mOkbuck

    BuckFileGenerator(Project rootProject) {
        mRootProject = rootProject
        mOkbuck = mRootProject.okbuck
    }

    /**
     * generate {@code BUCKFile}
     */
    Map<Project, BUCKFile> generate() {
        mOkbuck.buckProjects.each { Project project ->
            resolve(project)
        }

        ExperimentalExtension experimental = mOkbuck.experimental
        Map<Project, List<BuckRule>> projectRules = mOkbuck.buckProjects.collectEntries { Project project ->
            List<BuckRule> rules = createRules(project, experimental.espresso)
            [project, rules]
        }

        return projectRules.findAll { Project project, List<BuckRule> rules ->
            !rules.empty
        }.collectEntries { Project project, List<BuckRule> rules ->
            [project, new BUCKFile(rules)]
        } as Map<Project, BUCKFile>
    }

    private static void resolve(Project project) {
        Map<String, Target> targets = ProjectUtil.getTargets(project)

        targets.each { String name, Target target ->
            target.resolve()
        }
    }

    private static List<BuckRule> createRules(Project project, boolean espresso) {
        List<BuckRule> rules = []
        ProjectType projectType = ProjectUtil.getType(project)
        ProjectUtil.getTargets(project).each { String name, Target target ->
            switch (projectType) {
                case ProjectType.JAVA_LIB:
                    rules.addAll(createRules((JavaLibTarget) target))
                    break
                case ProjectType.JAVA_APP:
                    rules.addAll(createRules((JavaAppTarget) target))
                    break
                case ProjectType.ANDROID_LIB:
                    rules.addAll(createRules((AndroidLibTarget) target))
                    break
                case ProjectType.ANDROID_APP:
                    List<BuckRule> targetRules = createRules((AndroidAppTarget) target);
                    rules.addAll(targetRules)
                    if (espresso && ((AndroidAppTarget) target).instrumentationTestVariant) {
                        AndroidInstrumentationTarget instrumentationTarget =
                                new AndroidInstrumentationTarget(target.project,
                                        AndroidInstrumentationTarget.getInstrumentationTargetName(target.name))
                        rules.addAll(createRules(instrumentationTarget, (AndroidAppTarget) target, targetRules))
                    }
                    break
                default:
                    break
            }
        }

        return rules
    }

    private static List<BuckRule> createRules(JavaLibTarget target) {
        List<BuckRule> rules = []
        rules.add(JavaLibraryRuleComposer.compose(target))

        if (target.test.sources) {
            rules.add(JavaTestRuleComposer.compose(target))
        }
        return rules
    }

    private static List<BuckRule> createRules(JavaAppTarget target) {
        List<BuckRule> rules = []
        rules.addAll(createRules((JavaLibTarget) target))
        rules.add(JavaBinaryRuleComposer.compose(target))
        return rules
    }

    private static List<BuckRule> createRules(AndroidLibTarget target, String appClass = null, List<String> extraDeps = []) {
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
        androidLibRules.addAll(target.resources.collect { AndroidTarget.ResBundle resBundle ->
            AndroidResourceRuleComposer.compose(target, resBundle)
        })

        // BuildConfig
        androidLibRules.add(AndroidBuildConfigRuleComposer.compose(target))

        // Apt
        List<String> aptDeps = []
        if (!target.annotationProcessors.empty && !target.apt.externalDeps.empty) {
            AptRule aptRule = AptRuleComposer.compose(target)
            rules.add(aptRule)
            aptDeps.add(":${aptRule.name}")
        }
        aptDeps.addAll(BuckRuleComposer.targets(target.apt.targetDeps))

        // Jni
        androidLibRules.addAll(target.jniLibs.collect { String jniLib ->
            PreBuiltNativeLibraryRuleComposer.compose(target, jniLib)
        })

        List<String> deps = androidLibRules.collect { BuckRule rule ->
            ":${rule.name}"
        } as List<String>
        deps.addAll(extraDeps)

        // Gradle generate sources tasks
        List<GradleSourceGenRule> sourcegenRules = GradleSourceGenRuleComposer.compose(target, mOkbuck.gradle.absolutePath)
        List<ZipRule> zipRules = sourcegenRules.collect { GradleSourceGenRule sourcegenRule ->
            ZipRuleComposer.compose(sourcegenRule)
        }
        rules.addAll(sourcegenRules)
        rules.addAll(zipRules)
        Set<String> zipRuleNames = zipRules.collect { ZipRule rule ->
            ":${rule.name}"
        }

        // Lib
        androidLibRules.add(AndroidLibraryRuleComposer.compose(
                target,
                deps,
                aptDeps,
                aidlRuleNames,
                appClass,
                zipRuleNames
        ))

        // Test
        if (target.robolectric && target.test.sources) {
            androidLibRules.add(AndroidTestRuleComposer.compose(
                    target,
                    deps,
                    aptDeps,
                    aidlRuleNames,
                    appClass))
        }

        rules.addAll(androidLibRules)
        return rules

    }

    private static List<BuckRule> createRules(AndroidAppTarget target) {
        List<BuckRule> rules = []
        List<String> deps = [":${AndroidBuckRuleComposer.src(target)}"]

        Set<BuckRule> libRules = createRules((AndroidLibTarget) target,
                target.exopackage ? target.exopackage.appClass : null)
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
                    ExopackageAndroidLibraryRuleComposer.compose(target)
            rules.add(exoPackageRule)
            deps.add(":${exoPackageRule.name}")
        }

        rules.add(AndroidBinaryRuleComposer.compose(target, deps, ":${manifestRule.name}",
                keystoreRuleName))

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
