package com.uber.okbuck.core.model.base

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.uber.okbuck.core.util.FileUtil
import com.uber.okbuck.extension.OkBuckExtension
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.gradle.api.Project

/**
 * A target is roughly equivalent to what can be built with gradle via the various assemble tasks.
 *
 * For a project with no flavors and three build types - debug, release and development,
 * the possible variants are debug, release and development.
 * For a project with flavors flavor1 and flavor2 and three build types - debug, release and
 * development, the possible variants are flavor1Debug, flavor1Release, flavor1Development,
 * flavor2Debug, flavor2Release, flavor2Development.
 *
 * This class encapsulates all the data related to a variant to generate config files.
 */
@ToString(includes = ['project', 'name'])
@EqualsAndHashCode(includes = ["project", "name"])
abstract class Target {

    final Project project
    final Project rootProject
    final String name
    final String identifier
    final String path

    /**
     * Constructor.
     *
     * @param project The project.
     * @param name The target name.
     */
    Target(Project project, String name) {
        this.project = project
        this.name = name

        identifier = project.path.replaceFirst(':', '')
        path = identifier.replaceAll(':', File.separator)

        rootProject = project.gradle.rootProject
    }

    OkBuckExtension getOkbuck() {
        return rootProject.okbuck
    }

    protected Set<String> getAvailable(Collection<File> files) {
        return FileUtil.available(project, files)
    }

    protected <T> T getProp(Map<String, T> map, T defaultValue) {
        String nameKey = "${identifier}${name}" as String
        if (map.containsKey(nameKey)) {
            return map.get(nameKey)
        } else if (map.containsKey(identifier)) {
            return map.get(identifier)
        } else {
            return defaultValue
        }
    }

    Set<String> getExtraOpts(RuleType ruleType) {
        Map<String, Set<String>> propertyMap = getProp(okbuck.extraBuckOpts, ImmutableMap.of())
        return propertyMap.isEmpty() ? ImmutableSet.of()
                : propertyMap.computeIfAbsent(ruleType.name().toLowerCase(), { ImmutableSet.of() })
    }
}
