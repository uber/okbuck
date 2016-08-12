package com.github.okbuilds.okbuck

import org.gradle.api.Project

class OkBuckExtension {

    /**
     * build_tools_version
     */
    String buildToolVersion = "23.0.3"

    /**
     * Android target sdk version
     */
    String target = "android-23"

    /**
     * Annotation processor classes of project dependencies
     */
    Map<String, String> annotationProcessors = [:]

    /**
     * LinearAllocHardLimit used for multi-dex support.
     */
    Map<String, Integer> linearAllocHardLimit = [:]

    /**
     * Primary dex class patterns.
     */
    Map<String, List<String>> primaryDexPatterns = [:]

    /**
     * Configure the targets for project generation
     */
    Map<String, String> projectTargets = [:]

    /**
     * Whether to enable exopackage.
     */
    Map<String, Boolean> exopackage = [:]

    /**
     * Exopackage lib dependencies.
     */
    Map<String, List<String>> appLibDependencies = [:]

    /**
     * Set of projects to generate buck configs for. Default is all subprojects of root project.
     */
    Set<Project> buckProjects

    /**
     * List of files to remove when generating configuration.
     */
    List<String> remove = ['.buckconfig.local', "**/${OkBuckGradlePlugin.BUCK}"]

    /**
     * List of files to leave untouched when generating configuration.
     */
    List<String> keep = [".okbuck/**"]

    /**
     * Extra buck options
     */
    Map<String, Map<String, List<String>>> extraBuckOpts = [:]

    OkBuckExtension(Project project) {
        buckProjects = project.subprojects
    }
}
