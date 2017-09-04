package com.uber.okbuck.generator

import com.uber.okbuck.composer.android.AndroidBuckRuleComposer
import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.core.model.base.ProjectType
import com.uber.okbuck.core.model.base.Target
import com.uber.okbuck.core.util.ProjectUtil
import com.uber.okbuck.extension.OkBuckExtension
import com.uber.okbuck.template.config.BuckConfig
import org.gradle.api.Project
import org.jetbrains.annotations.Nullable

final class BuckConfigLocalGenerator {

    private BuckConfigLocalGenerator() {}

    /**
     * generate {@link BuckConfig}
     */
    static BuckConfig generate(OkBuckExtension okbuck,
                               @Nullable String groovyHome,
                               @Nullable String kotlinHome,
                               @Nullable String scalaHome,
                               @Nullable String proguardJar,
                               Set<String> defs) {
        Map<String, String> aliases = [:]
        okbuck.buckProjects.findAll { Project project ->
            ProjectUtil.getType(project) == ProjectType.ANDROID_APP
        }.each { Project project ->
            ProjectUtil.getTargets(project).each { String name, Target target ->
                aliases.put("${target.identifier.replaceAll(':', '-')}${name.capitalize()}" as String,
                        "//${target.path}:${AndroidBuckRuleComposer.bin((AndroidAppTarget) target)}" as String)
            }
        }

        return new BuckConfig()
                .aliases(aliases)
                .buildToolsVersion(okbuck.buildToolVersion)
                .target(okbuck.target)
                .groovyHome(groovyHome)
                .kotlinHome(kotlinHome)
                .scalaHome(scalaHome)
                .proguardJar(proguardJar)
                .defs(defs)
    }
}
