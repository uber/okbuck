package com.uber.okbuck.core.model.base;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.OkBuckExtension;
import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.gradle.api.Project;

/**
 * A target is roughly equivalent to what can be built with gradle via the various assemble tasks.
 *
 * <p>For a project with no flavors and three build types - debug, release and development, the
 * possible variants are debug, release and development. For a project with flavors flavor1 and
 * flavor2 and three build types - debug, release and development, the possible variants are
 * flavor1Debug, flavor1Release, flavor1Development, flavor2Debug, flavor2Release,
 * flavor2Development.
 *
 * <p>This class encapsulates all the data related to a variant to generate config files.
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
    // Replacement parameter of replaceAll should have backslashes escaped
    path = identifier.replaceAll(":", File.separator.replace("\\", "\\\\"));
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
    return ProjectUtil.getOkBuckExtension(project);
  }

  protected Set<String> getAvailable(Collection<File> files) {
    return FileUtil.available(project, files);
  }

  protected <T> T getProp(Map<String, T> map, T defaultValue) {
    return map.getOrDefault(
        getIdentifier() + getName(), map.getOrDefault(getIdentifier(), defaultValue));
  }

  public Collection<String> getExtraOpts(RuleType ruleType) {
    Map<String, Collection<String>> propertyMap =
        getProp(getOkbuck().extraBuckOpts, ImmutableMap.of());

    return propertyMap.isEmpty()
        ? ImmutableSet.of()
        : propertyMap.computeIfAbsent(ruleType.name().toLowerCase(), k -> ImmutableSet.of());
  }
}
