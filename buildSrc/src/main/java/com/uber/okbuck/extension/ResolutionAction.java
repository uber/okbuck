package com.uber.okbuck.extension;

public enum ResolutionAction {
  // Use latest version of the external dependency
  LATEST("latest"),

  // Use all resolved versions of the external dependency
  ALL("all"),

  // Use single resolved version of the external dependency and fail if multiple versions are found
  SINGLE("single");

  private final String action;

  ResolutionAction(String action) {
    this.action = action;
  }
}
