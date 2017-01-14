package com.uber.okbuck.extension;

import org.gradle.api.Project;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class OkBuckExtension {

    /**
     * Build Tools Version
     */
    public String buildToolVersion = "24.0.2";

    /**
     * Android target sdk version
     */
    public String target = "android-24";

    /**
     * Annotation processor classes of project dependencies
     */
    public Map<String, String> annotationProcessors = new HashMap<>();

    /**
     * LinearAllocHardLimit used for multi-dex support.
     */
    public Map<String, Integer> linearAllocHardLimit = new HashMap<>();

    /**
     * Primary dex class patterns.
     */
    public Map<String, List<String>> primaryDexPatterns = new HashMap<>();

    /**
     * Whether to enable exopackage.
     */
    public Map<String, Boolean> exopackage = new HashMap<>();

    /**
     * Exopackage lib dependencies.
     */
    public Map<String, List<String>> appLibDependencies = new HashMap<>();

    /**
     * Set of projects to generate buck configs for. Default is all subprojects of root project.
     */
    @SuppressWarnings("CanBeFinal")
    public Set<Project> buckProjects;

    /**
     * Extra buck options
     */
    public Map<String, Map<String, List<String>>> extraBuckOpts = new HashMap<>();

    /**
     * Whether to turn on buck's resource_union to reflect gradle's resource merging behavior
     */
    public boolean resourceUnion = true;

    public OkBuckExtension(Project project) {
        buckProjects = project.getSubprojects();
    }
}
