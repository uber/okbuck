package com.uber.okbuck.extension;

import org.gradle.api.Project;
import org.gradle.api.tasks.Input;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class OkBuckExtension {

    /**
     * Build Tools Version
     */
    @Input
    public String buildToolVersion = "24.0.2";

    /**
     * Android target sdk version
     */
    @Input
    public String target = "android-24";

    /**
     * Annotation processor classes of project dependencies
     */
    @Input
    public Map<String, String> annotationProcessors = new HashMap<>();

    /**
     * LinearAllocHardLimit used for multi-dex support.
     */
    @Input
    public Map<String, Integer> linearAllocHardLimit = new HashMap<>();

    /**
     * Primary dex class patterns.
     */
    @Input
    public Map<String, List<String>> primaryDexPatterns = new HashMap<>();

    /**
     * Whether to enable exopackage.
     */
    @Input
    public Map<String, Boolean> exopackage = new HashMap<>();

    /**
     * Exopackage lib dependencies.
     */
    @Input
    public Map<String, List<String>> appLibDependencies = new HashMap<>();

    /**
     * Set of projects to generate buck configs for. Default is all subprojects of root project.
     */
    @SuppressWarnings("CanBeFinal")
    @Input
    public Set<Project> buckProjects;

    /**
     * Extra buck options
     */
    @Input
    public Map<String, Map<String, List<String>>> extraBuckOpts = new HashMap<>();

    /** Extra buck defs **/
    @Input
    public Set<File> extraDefs = new HashSet<>();

    /**
     * Whether to turn on buck's resource_union to reflect gradle's resource merging behavior
     */
    @Input
    public boolean resourceUnion = true;

    /**
     * Additional dependency caches.
     * Every entry will create a new configuration "entryDepCache"
     * that can be used to fetch and cache dependencies.
     */
    @Input
    public Set<String> extraDepCaches = new HashSet<>();

    /**
    * Forces okbuck to fail if the project is using dynamic or snapshot dependencies
    */
    @Input
    public boolean failOnChangingDependencies = false;

    public OkBuckExtension(Project project) {
        buckProjects = project.getSubprojects();
    }
}
