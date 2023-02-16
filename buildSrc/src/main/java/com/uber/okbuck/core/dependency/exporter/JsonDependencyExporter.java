package com.uber.okbuck.core.dependency.exporter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.gradle.api.artifacts.ExternalDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

public class JsonDependencyExporter implements DependencyExporter {

  private static final Logger LOG = LoggerFactory.getLogger(JsonDependencyExporter.class);

  private final String filePath;
  private final boolean enable;

  public JsonDependencyExporter(String filePath, boolean enable) {
    this.filePath = filePath;
    this.enable = enable;
  }

  @Override
  public void export(Set<ExternalDependency> dependencies) {
    if (!enable) {
      LOG.info("Exporting dependencies is disabled");
      return;
    }

    Set<DependencyExporterModel> dependencyModels = dependencies.stream()
        .map(dependency -> new DependencyExporterModel(dependency))
        .collect(Collectors.toSet());

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    try {
      LOG.info("Exporting dependencies to JSON at " + Paths.get(filePath).toAbsolutePath());
      gson.toJson(dependencyModels, Files.newBufferedWriter(Paths.get(filePath), StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new ExporterException(e);
    }
  }
}
