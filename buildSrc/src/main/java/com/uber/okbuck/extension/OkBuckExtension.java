package com.uber.okbuck.extension;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;

import java.io.File;
import java.util.Collection;
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
    public Map<String, List<String>> annotationProcessors = new HashMap<>();

    /**
     * LinearAllocHardLimit used for multi-dex support.
     */
    @Input
    public Map<String, Integer> linearAllocHardLimit = new HashMap<>();

    /**
     * Primary dex class patterns.
     */
    @Input
    public Map<String, Set<String>> primaryDexPatterns = new HashMap<>();

    /**
     * Whether to enable exopackage.
     */
    @Input
    public Map<String, Boolean> exopackage = new HashMap<>();

    /**
     * Exopackage lib dependencies.
     */
    @Input
    public Map<String, Set<String>> appLibDependencies = new HashMap<>();

    /**
     * Proguard mapping file applied via applymapping
     */
    @Input
    public Map<String, File> proguardMappingFile = new HashMap<>();

    /**
     * List of build types/variant names for which to exclude generating lint rules
     */
    @Input
    public Map<String, Set<String>> lintExclude = new HashMap<>();

    /**
     * List of build types/variant names for which to exclude generating test rules
     */
    @Input
    public Map<String, Set<String>> testExclude = new HashMap<>();

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
    public Map<String, Map<String, Collection<String>>> extraBuckOpts = new HashMap<>();

    /**
     * Extra buck defs
     **/
    @Input
    public Set<File> extraDefs = new HashSet<>();

    /**
     * Whether to turn on buck's resource_union to reflect gradle's resource merging behavior
     */
    @Input
    public boolean resourceUnion = true;

    /**
     * Whether to generate android_build_config rules for library projects
     */
    @Input
    public boolean libraryBuildConfig = true;

    /**
     * List of exclude patterns for resources to be processed by aapt
     */
    @Input
    public Set<String> excludeResources = new HashSet<>();

    /**
     * Additional dependency caches.
     * Every value "entry" will create a new configuration "entryExtraDepCache"
     * that can be used to fetch and cache dependencies.
     */
    @Input
    public Set<String> extraDepCaches = new HashSet<>();

    /**
     * Forces okbuck to fail if the project is using dynamic or snapshot dependencies
     */
    @Input
    public boolean failOnChangingDependencies = false;

    /**
     * The prebult buck binary to use
     */
    @Input
    public String buckBinary = "com.github.facebook:buck:17aba06d14f0667460ee93953747421b01950dd7@pex";

    public OkBuckExtension(Project project) {
        buckProjects = project.getSubprojects();
    }

    private IntellijExtension intellijExtension = new IntellijExtension();
    private ExperimentalExtension experimentalExtension = new ExperimentalExtension();

    public void intellij(Action<IntellijExtension> container) {
        container.execute(intellijExtension);
    }

    public IntellijExtension getIntellijExtension() {
        return intellijExtension;
    }

    public void experimental(Action<ExperimentalExtension> container) {
        container.execute(experimentalExtension);
    }

    public ExperimentalExtension getExperimentalExtension() {
        return experimentalExtension;
    }
}
