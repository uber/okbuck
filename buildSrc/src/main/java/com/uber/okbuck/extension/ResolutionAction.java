package com.uber.okbuck.extension;

public enum ResolutionAction {
  // Use latest version of the external dependency
  LATEST,

  // Use all resolved versions of the external dependency
  ALL,

  // Use single resolved version of the external dependency and fail if multiple versions are found
  SINGLE;
}
