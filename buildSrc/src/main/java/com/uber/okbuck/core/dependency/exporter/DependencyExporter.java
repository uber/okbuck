package com.uber.okbuck.core.dependency.exporter;

import org.gradle.api.artifacts.ExternalDependency;

import java.util.Set;

public interface DependencyExporter {

  void export(Set<ExternalDependency> dependencies);
}
