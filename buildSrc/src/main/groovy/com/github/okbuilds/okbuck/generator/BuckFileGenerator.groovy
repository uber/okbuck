package com.github.okbuilds.okbuck.generator

import com.github.okbuilds.core.model.*
import com.github.okbuilds.core.util.ProjectUtil
import com.github.okbuilds.okbuck.OkBuckExtension
import com.github.okbuilds.okbuck.composer.*
import com.github.okbuilds.okbuck.config.BUCKFile
import com.github.okbuilds.okbuck.rule.*
import org.gradle.api.Project

final class BuckFileGenerator {

    private static final TARGET_DEBUG = 'debug'
    private static final TARGET_RELEASE = 'release'

    private final Project mRootProject
    private final OkBuckExtension mOkbuck

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

        Map<Project, List<BuckRule>> projectRules = mOkbuck.buckProjects.collectEntries { Project project ->
            List<BuckRule> rules = createRules(project)
            def projectConfigRule = createProjectConfigRule(project, mOkbuck.projectTargets)
            if (projectConfigRule != null) {
                rules.add(projectConfigRule)
            }
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

    private static List<BuckRule> createRules(Project project) {
        List<BuckRule> rules = []
        ProjectType projectType = ProjectUtil.getType(project)
        ProjectUtil.getTargets(project).each { String name, Target target ->
            switch (projectType) {
                case ProjectType.JAVA_LIB:
                    rules.addAll(createRules((JavaLibTarget) target))
                    break
                case ProjectType.ANDROID_LIB:
                    rules.addAll(createRules((AndroidLibTarget) target))
                    break
                case ProjectType.ANDROID_APP:
                    rules.addAll(createRules((AndroidAppTarget) target))
                    break
                default:
                    break
            }
        }

        return rules
    }

    private static ProjectConfigRule createProjectConfigRule(Project project, Map<String, String> projectConfigTargets) {
        def projectType = ProjectUtil.getType(project)
        switch (projectType) {
            case ProjectType.ANDROID_APP:
                def customBuildVariant = projectConfigTargets.get(project.name)
                def appTarget = (AndroidAppTarget) getTargetForVariant(
                        project,
                        customBuildVariant != null ? customBuildVariant : TARGET_DEBUG
                )
                return ProjectConfigComposer.composeAndroidApp(appTarget)
            case ProjectType.ANDROID_LIB:
                def customBuildVariant = projectConfigTargets.get(project.name)
                def libraryTarget = (AndroidLibTarget) getTargetForVariant(
                        project,
                        customBuildVariant != null ? customBuildVariant : TARGET_RELEASE
                )
                return ProjectConfigComposer.composeAndroidLibrary(libraryTarget)
            case ProjectType.JAVA_LIB:
                def libraryTarget = (JavaLibTarget) getTargetForVariant(project, JavaLibTarget.MAIN)
                return ProjectConfigComposer.composeJavaLibrary(libraryTarget)
            default:
                return null
        }
    }

    private static List<BuckRule> createRules(JavaLibTarget target) {
        List<BuckRule> rules = []
        rules.add(JavaLibraryRuleComposer.compose(target))

        if (target.test.sources) {
            rules.add(JavaTestRuleComposer.compose(target))
        }
        return rules
    }

    private static List<BuckRule> createRules(AndroidLibTarget target, String appClass = null) {
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
        }

        // Lib
        androidLibRules.add(AndroidLibraryRuleComposer.compose(
                target,
                deps,
                aptDeps,
                aidlRuleNames,
                appClass))

        // Test
        if (target.test.sources) {
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

    private static Target getTargetForVariant(Project project, String desiredVariant) {
        def target = ProjectUtil.getTargets(project).get(desiredVariant)
        if (target == null) {
            throw new IllegalStateException("Unable to infer default project target for ${project.name} (tried looking " +
                    "for ${desiredVariant}), if you are using a custom build variants add a projectTarget entry to " +
                    "your root build.gradle.")
        }

        return target
    }
}
