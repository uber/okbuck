package com.uber.okbuck.core.dependency;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Splitter;
import java.util.List;
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
    List<String> parts = Splitter.on(COORD_DELIMITER).splitToList(s);
    VersionlessDependency versionless;

    switch (parts.size()) {
      case 2:
        versionless =
            VersionlessDependency.builder().setGroup(parts.get(0)).setName(parts.get(1)).build();
        break;
      case 3:
        versionless =
            VersionlessDependency.builder()
                .setGroup(parts.get(0))
                .setName(parts.get(1))
                .setClassifier(parts.get(2))
                .build();
        break;
      default:
        throw new RuntimeException("Invalid dependency specified: " + s);
    }
    return versionless;
  }
}
