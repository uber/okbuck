package com.github.okbuilds.okbuck.generator

import com.github.okbuilds.core.model.ProjectType
import com.github.okbuilds.core.model.Target
import com.github.okbuilds.core.util.ProjectUtil
import com.github.okbuilds.okbuck.OkBuckExtension
import com.github.okbuilds.okbuck.composer.AndroidBuckRuleComposer
import com.github.okbuilds.okbuck.config.DotBuckConfigLocalFile
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
                aliases.put("${target.path.replaceAll(':', '-')}${name.capitalize()}",
                        "//${target.path}:${AndroidBuckRuleComposer.bin(target)}")
            }
        }

        return new DotBuckConfigLocalFile(aliases, okbuck.buildToolVersion, okbuck.target, [".git", "**/.svn"])
    }
}
