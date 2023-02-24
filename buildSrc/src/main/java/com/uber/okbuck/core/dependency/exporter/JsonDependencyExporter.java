package com.uber.okbuck.core.dependency.exporter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.uber.okbuck.extension.ExportDependenciesExtension;
import org.gradle.api.artifacts.ExternalDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

public class JsonDependencyExporter implements DependencyExporter {

  private static final Logger LOG = LoggerFactory.getLogger(JsonDependencyExporter.class);

  private final ExportDependenciesExtension exportDependenciesExtension;

  public JsonDependencyExporter(ExportDependenciesExtension exportDependenciesExtension) {
    this.exportDependenciesExtension = exportDependenciesExtension;
  }

  private static void createParent(Path path){
    try{
      Files.createDirectories(path.getParent());
    } catch (IOException e) {
      throw new ExporterException(e);
    }
  }

  @Override
  public void export(Set<ExternalDependency> dependencies) {
    if (!exportDependenciesExtension.isEnabled()) {
      LOG.info("Exporting dependencies is disabled");
      return;
    }

    Set<DependencyExporterModel> dependencyModels =
        dependencies.stream()
            .map(dependency -> new DependencyExporterModel(dependency))
            .collect(Collectors.toSet());

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String file = exportDependenciesExtension.getFile();
    Path path = Paths.get(file);
    createParent(path);
    try (BufferedWriter writer =
        Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      LOG.info("Exporting dependencies to JSON at " + file);
      gson.toJson(dependencyModels, writer);
    } catch (IOException e) {
      throw new ExporterException(e);
    }
  }
}
