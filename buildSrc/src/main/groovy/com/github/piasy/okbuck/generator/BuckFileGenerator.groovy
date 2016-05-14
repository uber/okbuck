/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Piasy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.piasy.okbuck.generator

import com.github.piasy.okbuck.OkBuckExtension
import com.github.piasy.okbuck.composer.AndroidBinaryRuleComposer
import com.github.piasy.okbuck.composer.AndroidBuildConfigRuleComposer
import com.github.piasy.okbuck.composer.AndroidLibraryRuleComposer
import com.github.piasy.okbuck.composer.AndroidManifestRuleComposer
import com.github.piasy.okbuck.composer.AndroidResourceRuleComposer
import com.github.piasy.okbuck.composer.AptRuleComposer
import com.github.piasy.okbuck.composer.ExopackageAndroidLibraryRuleComposer
import com.github.piasy.okbuck.composer.GenAidlRuleComposer
import com.github.piasy.okbuck.composer.JavaLibraryRuleComposer
import com.github.piasy.okbuck.composer.KeystoreRuleComposer
import com.github.piasy.okbuck.composer.PreBuiltNativeLibraryRuleComposer
import com.github.piasy.okbuck.config.BUCKFile
import com.github.piasy.okbuck.model.AndroidAppTarget
import com.github.piasy.okbuck.model.AndroidLibTarget
import com.github.piasy.okbuck.model.AndroidTarget
import com.github.piasy.okbuck.model.JavaLibTarget
import com.github.piasy.okbuck.model.ProjectType
import com.github.piasy.okbuck.model.Target
import com.github.piasy.okbuck.rule.AndroidManifestRule
import com.github.piasy.okbuck.rule.AndroidResourceRule
import com.github.piasy.okbuck.rule.AptRule
import com.github.piasy.okbuck.rule.BuckRule
import com.github.piasy.okbuck.rule.ExopackageAndroidLibraryRule
import com.github.piasy.okbuck.rule.GenAidlRule
import com.github.piasy.okbuck.util.ProjectUtil
import org.gradle.api.Project

/**
 * Created by Piasy{github.com/Piasy} on 15/10/6.
 *
 * used to generate BUCK file content.
 */
final class BuckFileGenerator {

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
        Map<Project, List<BuckRule>> projectRules = mOkbuck.buckProjects.collectEntries { Project project ->
            [project, createRules(project)]
        }

        return projectRules.findAll { Project project, List<BuckRule> rules ->
            !rules.empty
        }.collectEntries { Project project, List<BuckRule> rules ->
            [project, new BUCKFile(rules)]
        } as Map<Project, BUCKFile>
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

    private static List<BuckRule> createRules(JavaLibTarget target) {
        return [JavaLibraryRuleComposer.compose(target)]
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
        if (!target.aptDeps.empty) {
            AptRule aptRule = AptRuleComposer.compose(target)
            rules.add(aptRule)
            aptDeps.add(":${aptRule.name}")
        }
        aptDeps.addAll(target.targetAptDeps.collect { Target targetDep ->
            "//${targetDep.path}:src_${targetDep.name}"
        })

        // Jni
        androidLibRules.addAll(target.jniLibs.collect { String jniLib ->
            PreBuiltNativeLibraryRuleComposer.compose(target, jniLib)
        })

        List<String> deps = androidLibRules.collect { BuckRule rule ->
            ":${rule.name}"
        }

        // Lib
        androidLibRules.add(AndroidLibraryRuleComposer.compose(target, deps, aptDeps, aidlRuleNames, appClass))

        rules.addAll(androidLibRules)
        return rules

    }

    private static List<BuckRule> createRules(AndroidAppTarget target) {
        List<BuckRule> rules = []
        List<String> deps = [":src_${target.name}"]

        Set<BuckRule> libRules = createRules((AndroidLibTarget) target,
                target.exopackage ? target.appClass : null)
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
}
