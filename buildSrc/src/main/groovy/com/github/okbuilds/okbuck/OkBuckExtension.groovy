package com.github.okbuilds.okbuck

import org.gradle.api.Project

class OkBuckExtension {

    /**
     * build_tools_version
     */
    String buildToolVersion = "24.0.2"

    /**
     * Android target sdk version
     */
    String target = "android-24"

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
     * Extra buck options
     */
    Map<String, Map<String, List<String>>> extraBuckOpts = [:]

    File gradle

    OkBuckExtension(Project project) {
        buckProjects = project.subprojects
        gradle = project.file("gradlew")
    }
}
