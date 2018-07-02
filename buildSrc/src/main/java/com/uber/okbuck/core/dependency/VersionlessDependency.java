package com.uber.okbuck.core.dependency;

import java.util.Objects;
import java.util.Optional;

public final class VersionlessDependency {

  static final String COORD_DELIMITER = ":";

  private final String group;
  private final String name;
  private final Optional<String> classifier;

  public VersionlessDependency(String group, String name, Optional<String> classifier) {
    this.group = group;
    this.name = name;
    this.classifier = classifier;
  }

  public VersionlessDependency(String group, String name) {
    this(group, name, Optional.empty());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    VersionlessDependency that = (VersionlessDependency) o;

    return group.equals(that.group) && name.equals(that.name) && classifier.equals(that.classifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(group, name, classifier);
  }

  @Override
  public String toString() {
    return this.group
        + COORD_DELIMITER
        + this.name
        + classifier.map(c -> COORD_DELIMITER + c).orElse("");
  }
}
