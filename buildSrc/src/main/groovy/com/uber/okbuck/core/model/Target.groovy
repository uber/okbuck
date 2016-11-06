package com.uber.okbuck.core.model

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
    final OkBuckExtension okbuck

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
        path = identifier.replaceAll(':', '/')

        rootProject = project.gradle.rootProject
        okbuck = rootProject.okbuck
    }

    protected Set<String> getAvailable(Collection<File> files) {
        return files.findAll { File file ->
            file.exists()
        }.collect { File file ->
            FileUtil.getRelativePath(project.projectDir, file)
        }
    }

    def getProp(Map map, defaultValue) {
        return map.get("${identifier}${name}", map.get(identifier, defaultValue))
    }

    public void resolve() {
        // no scope to resolve
    }
}
