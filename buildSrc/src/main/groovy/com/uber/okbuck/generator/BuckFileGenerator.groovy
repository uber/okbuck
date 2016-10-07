package com.uber.okbuck.generator

import com.uber.okbuck.core.util.ProjectUtil
import com.uber.okbuck.ExperimentalExtension
import com.uber.okbuck.OkBuckExtension
import com.uber.okbuck.config.BUCKFile
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
        Map<Project, List<com.uber.okbuck.rule.BuckRule>> projectRules = mOkbuck.buckProjects.collectEntries { Project project ->
            List<com.uber.okbuck.rule.BuckRule> rules = createRules(project, experimental.espresso)
            [project, rules]
        }

        return projectRules.findAll { Project project, List<com.uber.okbuck.rule.BuckRule> rules ->
            !rules.empty
        }.collectEntries { Project project, List<com.uber.okbuck.rule.BuckRule> rules ->
            [project, new BUCKFile(rules)]
        } as Map<Project, BUCKFile>
    }

    private static void resolve(Project project) {
        Map<String, com.uber.okbuck.core.model.Target> targets = ProjectUtil.getTargets(project)

        targets.each { String name, com.uber.okbuck.core.model.Target target ->
            target.resolve()
        }
    }

    private static List<com.uber.okbuck.rule.BuckRule> createRules(Project project, boolean espresso) {
        List<com.uber.okbuck.rule.BuckRule> rules = []
        com.uber.okbuck.core.model.ProjectType projectType = ProjectUtil.getType(project)
        ProjectUtil.getTargets(project).each { String name, com.uber.okbuck.core.model.Target target ->
            switch (projectType) {
                case com.uber.okbuck.core.model.ProjectType.JAVA_LIB:
                    rules.addAll(createRules((com.uber.okbuck.core.model.JavaLibTarget) target))
                    break
                case com.uber.okbuck.core.model.ProjectType.JAVA_APP:
                    rules.addAll(createRules((com.uber.okbuck.core.model.JavaAppTarget) target))
                    break
                case com.uber.okbuck.core.model.ProjectType.ANDROID_LIB:
                    rules.addAll(createRules((com.uber.okbuck.core.model.AndroidLibTarget) target))
                    break
                case com.uber.okbuck.core.model.ProjectType.ANDROID_APP:
                    List<com.uber.okbuck.rule.BuckRule> targetRules = createRules((com.uber.okbuck.core.model.AndroidAppTarget) target)
                    rules.addAll(targetRules)
                    if (espresso && ((com.uber.okbuck.core.model.AndroidAppTarget) target).instrumentationTestVariant) {
                        com.uber.okbuck.core.model.AndroidInstrumentationTarget instrumentationTarget =
                                new com.uber.okbuck.core.model.AndroidInstrumentationTarget(target.project,
                                        com.uber.okbuck.core.model.AndroidInstrumentationTarget.getInstrumentationTargetName(target.name))
                        rules.addAll(createRules(instrumentationTarget, (com.uber.okbuck.core.model.AndroidAppTarget) target, targetRules))
                    }
                    break
                default:
                    break
            }
        }

        // de-dup rules by name
        rules = rules.unique { rule ->
            rule.name
        }

        return rules
    }

    private static List<com.uber.okbuck.rule.BuckRule> createRules(com.uber.okbuck.core.model.JavaLibTarget target) {
        List<com.uber.okbuck.rule.BuckRule> rules = []
        rules.add(com.uber.okbuck.composer.JavaLibraryRuleComposer.compose(
                target,
                mOkbuck.postProcessClassesCommands))

        if (target.test.sources) {
            rules.add(com.uber.okbuck.composer.JavaTestRuleComposer.compose(
                    target,
                    mOkbuck.postProcessClassesCommands))
        }
        return rules
    }

    private static List<com.uber.okbuck.rule.BuckRule> createRules(com.uber.okbuck.core.model.JavaAppTarget target) {
        List<com.uber.okbuck.rule.BuckRule> rules = []
        rules.addAll(createRules((com.uber.okbuck.core.model.JavaLibTarget) target))
        rules.add(com.uber.okbuck.composer.JavaBinaryRuleComposer.compose(target))
        return rules
    }

    private static List<com.uber.okbuck.rule.BuckRule> createRules(com.uber.okbuck.core.model.AndroidLibTarget target, String appClass = null, List<String> extraDeps = []) {
        List<com.uber.okbuck.rule.BuckRule> rules = []
        List<com.uber.okbuck.rule.BuckRule> androidLibRules = []

        // Aidl
        List<com.uber.okbuck.rule.BuckRule> aidlRules = target.aidl.collect { String aidlDir ->
            com.uber.okbuck.composer.GenAidlRuleComposer.compose(target, aidlDir)
        }
        List<String> aidlRuleNames = aidlRules.collect { com.uber.okbuck.rule.GenAidlRule rule ->
            ":${rule.name}"
        }
        androidLibRules.addAll(aidlRules)

        // Res
        androidLibRules.addAll(target.resources.collect { com.uber.okbuck.core.model.AndroidTarget.ResBundle resBundle ->
            com.uber.okbuck.composer.AndroidResourceRuleComposer.compose(target, resBundle)
        })

        // BuildConfig
        androidLibRules.add(com.uber.okbuck.composer.AndroidBuildConfigRuleComposer.compose(target))

        // Apt
        List<String> aptDeps = []
        if (!target.annotationProcessors.empty && !target.apt.externalDeps.empty) {
            com.uber.okbuck.rule.AptRule aptRule = com.uber.okbuck.composer.AptRuleComposer.compose(target)
            rules.add(aptRule)
            aptDeps.add(":${aptRule.name}")
        }
        aptDeps.addAll(com.uber.okbuck.composer.BuckRuleComposer.targets(target.apt.targetDeps))

        // Jni
        androidLibRules.addAll(target.jniLibs.collect { String jniLib ->
            com.uber.okbuck.composer.PreBuiltNativeLibraryRuleComposer.compose(target, jniLib)
        })

        List<String> deps = androidLibRules.collect { com.uber.okbuck.rule.BuckRule rule ->
            ":${rule.name}"
        } as List<String>
        deps.addAll(extraDeps)

        // Gradle generate sources tasks
        List<com.uber.okbuck.rule.GradleSourceGenRule> sourcegenRules = com.uber.okbuck.composer.GradleSourceGenRuleComposer.compose(target, mOkbuck.gradle.absolutePath)
        List<com.uber.okbuck.rule.ZipRule> zipRules = sourcegenRules.collect { com.uber.okbuck.rule.GradleSourceGenRule sourcegenRule ->
            com.uber.okbuck.composer.ZipRuleComposer.compose(sourcegenRule)
        }
        rules.addAll(sourcegenRules)
        rules.addAll(zipRules)
        Set<String> zipRuleNames = zipRules.collect { com.uber.okbuck.rule.ZipRule rule ->
            ":${rule.name}"
        }

        // Lib
        androidLibRules.add(com.uber.okbuck.composer.AndroidLibraryRuleComposer.compose(
                target,
                deps,
                aptDeps,
                aidlRuleNames,
                mOkbuck.postProcessClassesCommands,
                appClass,
                zipRuleNames
        ))

        // Test
        if (target.robolectric && target.test.sources) {
            androidLibRules.add(com.uber.okbuck.composer.AndroidTestRuleComposer.compose(
                    target,
                    deps,
                    aptDeps,
                    aidlRuleNames,
                    mOkbuck.postProcessClassesCommands,
                    appClass))
        }

        rules.addAll(androidLibRules)
        return rules

    }

    private static List<com.uber.okbuck.rule.BuckRule> createRules(com.uber.okbuck.core.model.AndroidAppTarget target) {
        List<com.uber.okbuck.rule.BuckRule> rules = []
        List<String> deps = [":${com.uber.okbuck.composer.AndroidBuckRuleComposer.src(target)}"]

        Set<com.uber.okbuck.rule.BuckRule> libRules = createRules((com.uber.okbuck.core.model.AndroidLibTarget) target,
                target.exopackage ? target.exopackage.appClass : null)
        rules.addAll(libRules)

        libRules.each { com.uber.okbuck.rule.BuckRule rule ->
            if (rule instanceof com.uber.okbuck.rule.AndroidResourceRule && rule.name != null) {
                deps.add(":${rule.name}")
            }
        }

        com.uber.okbuck.rule.AndroidManifestRule manifestRule = com.uber.okbuck.composer.AndroidManifestRuleComposer.compose(target)
        rules.add(manifestRule)

        String keystoreRuleName = com.uber.okbuck.composer.KeystoreRuleComposer.compose(target)

        if (target.exopackage) {
            com.uber.okbuck.rule.ExopackageAndroidLibraryRule exoPackageRule =
                    com.uber.okbuck.composer.ExopackageAndroidLibraryRuleComposer.compose(
                            target,
                            mOkbuck.postProcessClassesCommands)
            rules.add(exoPackageRule)
            deps.add(":${exoPackageRule.name}")
        }

        rules.add(com.uber.okbuck.composer.AndroidBinaryRuleComposer.compose(target, deps, ":${manifestRule.name}",
                keystoreRuleName))

        return rules
    }

    private static List<com.uber.okbuck.rule.BuckRule> createRules(com.uber.okbuck.core.model.AndroidInstrumentationTarget target, com.uber.okbuck.core.model.AndroidAppTarget mainApkTarget,
                                                                   List<com.uber.okbuck.rule.BuckRule> mainApkTargetRules) {
        List<com.uber.okbuck.rule.BuckRule> rules = []

        Set<com.uber.okbuck.rule.BuckRule> libRules = createRules((com.uber.okbuck.core.model.AndroidLibTarget) target, null, filterAndroidDepRules(mainApkTargetRules))
        rules.addAll(libRules)

        com.uber.okbuck.rule.AndroidManifestRule manifestRule = com.uber.okbuck.composer.AndroidManifestRuleComposer.compose(target, target.instrumentation)
        rules.add(manifestRule)

        rules.add(com.uber.okbuck.composer.AndroidInstrumentationApkRuleComposer.compose(filterAndroidDepRules(rules), ":${manifestRule.name}", mainApkTarget))
        rules.add(com.uber.okbuck.composer.AndroidInstrumentationTestRuleComposer.compose(mainApkTarget))
        return rules
    }

    private static List<String> filterAndroidDepRules(List<com.uber.okbuck.rule.BuckRule> rules) {
        return rules.findAll { com.uber.okbuck.rule.BuckRule rule ->
            rule instanceof com.uber.okbuck.rule.AndroidLibraryRule || rule instanceof com.uber.okbuck.rule.AndroidResourceRule
        }.collect {
            ":${it.name}"
        }
    }
}
