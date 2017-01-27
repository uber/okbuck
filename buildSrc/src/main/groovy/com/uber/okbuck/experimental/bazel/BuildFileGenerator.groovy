package com.uber.okbuck.experimental.bazel

import com.uber.okbuck.config.BUCKFile
import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.core.model.android.AndroidLibTarget
import com.uber.okbuck.core.model.base.ProjectType
import com.uber.okbuck.core.model.java.JavaAppTarget
import com.uber.okbuck.core.model.java.JavaLibTarget
import com.uber.okbuck.core.util.ProjectUtil
import com.uber.okbuck.extension.OkBuckExtension
import com.uber.okbuck.rule.base.BuckRule
import org.gradle.api.Project

import static com.uber.okbuck.core.util.ProjectUtil.getTargets

final class BuildFileGenerator {
    static Map<Project, BUCKFile> generate(OkBuckExtension okBuckExt) {
        okBuckExt.buckProjects.<Project, List<BuckRule>, Project> collectEntries { project ->
            [project, createRules(project)]
        }.findAll { project, rules ->
            !rules.empty
        }.collectEntries { project, rules ->
            [project, new BUCKFile(rules)]
        } as Map<Project, BUCKFile>
    }

    // Library targets create one *_library rule. App targets create one *_library rule containing
    // the sources and resources and one *_binary target that depends on the library target.
    private static final targetHandlers = [
            (ProjectType.JAVA_LIB)   : { target, rules ->
                rules << BazelJavaLibraryRuleComposer.compose(target as JavaLibTarget) },
            (ProjectType.JAVA_APP)   : { target, rules ->
                rules << BazelJavaLibraryRuleComposer.compose(target as JavaLibTarget)
                rules << BazelJavaBinaryRuleComposer.compose(target as JavaAppTarget) },
            (ProjectType.ANDROID_LIB): { target, rules ->
                rules << BazelAndroidLibraryRuleComposer.compose(target as AndroidLibTarget) },
            (ProjectType.ANDROID_APP): { target, rules ->
                rules << BazelAndroidLibraryRuleComposer.compose(target as AndroidLibTarget)
                rules << BazelAndroidBinaryRuleComposer.compose(target as AndroidAppTarget) }]

    private static List<BuckRule> createRules(Project project) {
        getTargets(project).values().inject([]) { rules, target ->
            targetHandlers[ProjectUtil.getType(project)](target, rules)
        }
    }
}
