package com.uber.okbuck.core.dependency;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import java.util.Optional;

@AutoValue
public abstract class VersionlessDependency {

  static final String COORD_DELIMITER = ":";

  public abstract String group();

  public abstract String name();

  public abstract Optional<String> classifier();

  public static Builder builder() {
    return new AutoValue_VersionlessDependency.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setGroup(String value);

    public abstract Builder setName(String value);

    public abstract Builder setClassifier(Optional<String> value);

    public abstract Builder setClassifier(String value);

    public abstract VersionlessDependency build();
  }

  @Memoized
  public String mavenCoords() {
    return group()
        + COORD_DELIMITER
        + name()
        + classifier().map(c -> COORD_DELIMITER + c).orElse("");
  }

  public static VersionlessDependency fromMavenCoords(String s) {
    String[] parts = s.split(COORD_DELIMITER);
    VersionlessDependency versionless;

    switch (parts.length) {
      case 2:
        versionless = VersionlessDependency.builder().setGroup(parts[0]).setName(parts[1]).build();
        break;
      case 3:
        versionless =
            VersionlessDependency.builder()
                .setGroup(parts[0])
                .setName(parts[1])
                .setClassifier(parts[2])
                .build();
        break;
      default:
        throw new RuntimeException("Invalid dependency specified: " + s);
    }
    return versionless;
  }
}
