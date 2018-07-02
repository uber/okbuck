package com.uber.okbuck.extension;

import com.uber.okbuck.core.dependency.VersionlessDependency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ExternalExtension {

  /** Stores the dependencies which are allowed to have multiple versions. */
  private Map<String, List<String>> allowed = new HashMap<>();

  private Map<VersionlessDependency, List<String>> versionlessAllowedMap;

  public synchronized Map<VersionlessDependency, List<String>> getAllowedMap() {
    if (versionlessAllowedMap == null) {
      versionlessAllowedMap =
          allowed
              .entrySet()
              .stream()
              .collect(
                  Collectors.toMap(entry -> getVersionless(entry.getKey()), Map.Entry::getValue));
    }
    return versionlessAllowedMap;
  }

  public boolean isAllowed(VersionlessDependency versionless) {
    return getAllowedMap().containsKey(versionless);
  }

  private VersionlessDependency getVersionless(String s) {
    String[] parts = s.split(":");
    VersionlessDependency versionless;
    if (parts.length == 2) {
      versionless = new VersionlessDependency(parts[0], parts[1]);
    } else if (parts.length == 3) {
      versionless = new VersionlessDependency(parts[0], parts[1], Optional.of(parts[2]));
    } else {
      throw new RuntimeException("Invalid dependency specified: " + s);
    }
    return versionless;
  }
}
