package com.uber.okbuck.generator

import com.uber.okbuck.OkBuckExtension
import com.uber.okbuck.composer.AndroidBuckRuleComposer
import com.uber.okbuck.config.DotBuckConfigLocalFile
import com.uber.okbuck.core.model.AndroidAppTarget
import com.uber.okbuck.core.model.ProjectType
import com.uber.okbuck.core.model.Target
import com.uber.okbuck.core.util.ProjectUtil
import org.gradle.api.Project

final class DotBuckConfigLocalGenerator {

    private DotBuckConfigLocalGenerator() {}

    /**
     * generate {@link DotBuckConfigLocalFile}
     */
    static DotBuckConfigLocalFile generate(OkBuckExtension okbuck) {
        Map<String, String> aliases = [:]
        okbuck.buckProjects.findAll { Project project ->
            ProjectUtil.getType(project) == ProjectType.ANDROID_APP
        }.each { Project project ->
            ProjectUtil.getTargets(project).each { String name, Target target ->
                aliases.put("${target.identifier.replaceAll(':', '-')}${name.capitalize()}",
                        "//${target.path}:${AndroidBuckRuleComposer.bin((AndroidAppTarget) target)}")
            }
        }

        return new DotBuckConfigLocalFile(aliases, okbuck.buildToolVersion, okbuck.target, [".git", "**/.svn"])
    }
}
