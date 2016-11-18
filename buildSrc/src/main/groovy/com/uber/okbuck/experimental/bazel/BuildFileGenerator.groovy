package com.uber.okbuck.experimental.bazel

import com.uber.okbuck.config.BUCKFile
import com.uber.okbuck.core.model.AndroidAppTarget
import com.uber.okbuck.core.model.AndroidLibTarget
import com.uber.okbuck.core.model.JavaAppTarget
import com.uber.okbuck.core.model.JavaLibTarget
import com.uber.okbuck.core.model.ProjectType
import com.uber.okbuck.core.model.Target
import com.uber.okbuck.core.util.ProjectUtil
import com.uber.okbuck.rule.BuckRule
import org.gradle.api.Project

import static com.uber.okbuck.core.util.ProjectUtil.getTargets

final class BuildFileGenerator {
    static Map<Project, BUCKFile> generate(Project rootProject) {
        rootProject.okbuck.buckProjects.each { Project project ->
            getTargets(project).each { String name, Target target ->
                target.resolve()
            }
        }

        def projectRules = rootProject.okbuck.buckProjects.collectEntries { Project project ->
            [project, createRules(project)]
        }

        return projectRules.findAll { Project project, List<BuckRule> rules ->
            !rules.empty
        }.collectEntries { Project project, List<BuckRule> rules ->
            [project, new BUCKFile(rules)]
        } as Map<Project, BUCKFile>
    }

    // Library targets create one *_library rule. App targets create one *_library rule containing
    // the sources and resources and one *_binary target that depends on the library target.
    private static final def targetHandlers = [
            (ProjectType.JAVA_LIB)   : { target, rules ->
                rules << JavaLibraryRuleComposer.compose(target as JavaLibTarget) },
            (ProjectType.JAVA_APP)   : { target, rules ->
                rules << JavaLibraryRuleComposer.compose(target as JavaLibTarget)
                rules << JavaBinaryRuleComposer.compose(target as JavaAppTarget) },
            (ProjectType.ANDROID_LIB): { target, rules ->
                rules << AndroidLibraryRuleComposer.compose(target as AndroidLibTarget) },
            (ProjectType.ANDROID_APP): { target, rules ->
                rules << AndroidLibraryRuleComposer.compose(target as AndroidLibTarget)
                rules << AndroidBinaryRuleComposer.compose(target as AndroidAppTarget) }]

    private static def createRules(Project project) {
        getTargets(project).values().inject([]) { rules, target ->
            targetHandlers[ProjectUtil.getType(project)](target, rules)
        }
    }
}
