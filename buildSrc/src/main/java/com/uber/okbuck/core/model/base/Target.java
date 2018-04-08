package com.uber.okbuck.core.model.base;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.extension.OkBuckExtension;

import org.gradle.api.Project;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

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
public abstract class Target {

    private final Project project;
    private final Project rootProject;
    private final String name;
    private final String identifier;
    private final String path;

    public Target(Project project, String name) {
        this.project = project;
        this.name = name;
        identifier = project.getPath().replaceFirst(":", "");
        path = identifier.replaceAll(":", File.separator);
        rootProject = project.getRootProject();
    }

    public Project getProject() {
        return project;
    }

    public Project getRootProject() {
        return rootProject;
    }

    public String getName() {
        return name;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getPath() {
        return path;
    }

    public OkBuckExtension getOkbuck() {
        return rootProject.getExtensions().getByType(OkBuckExtension.class);
    }

    protected Set<String> getAvailable(Collection<File> files) {
        return FileUtil.available(project, files);
    }

    protected <T> T getProp(Map<String, T> map, T defaultValue) {
        return map.getOrDefault(identifier + name, map.getOrDefault(identifier, defaultValue));
    }

    public Set<String> getExtraOpts(RuleType ruleType) {
        Map<String, Set<String>> propertyMap = getProp(getOkbuck().extraBuckOpts, ImmutableMap.of());
        return propertyMap.isEmpty() ? ImmutableSet.of()
                : propertyMap.computeIfAbsent(ruleType.name().toLowerCase(), k -> ImmutableSet.of());
    }
}
