package com.uber.okbuck.core.model.android;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.core.dependency.ExternalDependency;
import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.core.model.base.Target;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.XmlUtil;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.gradle.api.Project;
import org.gradle.api.file.FileTree;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ExoPackageScope extends Scope {

  private final Scope base;
  private final String manifest;

  ExoPackageScope(
      Project project, Scope base, List<String> exoPackageDependencies, String manifest) {
    super(project, null, ImmutableSet.of(), ImmutableSet.of(), base.getCompilerOptions());
    this.base = base;
    this.manifest = manifest;
    extractDependencies(base, exoPackageDependencies);
  }

  @Nullable
  public String getAppClass() {
    String appClass = null;

    File manifestFile = project.getRootProject().file(manifest);
    Document manifestXml = XmlUtil.loadXml(manifestFile);
    try {
      NodeList nodeList = manifestXml.getElementsByTagName("application");
      Preconditions.checkArgument(nodeList.getLength() == 1);

      Element application = (Element) nodeList.item(0);

      appClass = application.getAttribute("android:name");
      appClass = appClass.replaceAll("\\.", "/").replaceAll("^/", "");
    } catch (Exception ignored) {
    }

    final String finalAppClass = appClass;

    if (appClass != null && !appClass.isEmpty()) {
      Optional<String> optionalAppClass =
          base.getSources()
              .stream()
              .map(
                  sourceDir -> {
                    FileTree found =
                        project.fileTree(
                            ImmutableMap.of(
                                "dir",
                                sourceDir,
                                "includes",
                                ImmutableList.of("**/" + finalAppClass + ".java")));
                    try {
                      return FileUtil.getRelativePath(
                          project.getProjectDir(), found.getSingleFile());
                    } catch (IllegalStateException ignored) {
                      return null;
                    }
                  })
              .filter(Objects::nonNull)
              .findFirst();

      if (optionalAppClass.isPresent()) {
        return optionalAppClass.get();
      }
    }
    return finalAppClass;
  }

  private void extractDependencies(Scope base, List<String> exoPackageDependencies) {
    exoPackageDependencies.forEach(
        exoPackageDep -> {
          String first; // can denote either group or project name
          String last; // can denote either module or configuration name
          boolean qualified = false;

          if (exoPackageDep.contains(":")) {
            List<String> parts = Splitter.on(':').splitToList(exoPackageDep);
            first = parts.get(0);
            last = parts.get(1);
            qualified = true;
          } else {
            first = last = exoPackageDep;
          }

          final boolean fullyQualified = qualified;

          Optional<ExternalDependency> externalDepOptional =
              base.getExternal()
                  .stream()
                  .filter(
                      dependency -> {
                        boolean match = true;
                        if (fullyQualified) {
                          match = dependency.getGroup().equals(first);
                        }
                        match &= dependency.getName().equals(last);
                        return match;
                      })
                  .findFirst();

          if (externalDepOptional.isPresent()) {
            getExternal().add(externalDepOptional.get());
          } else {
            Optional<Target> variantDepOptional =
                base.getTargetDeps()
                    .stream()
                    .filter(
                        variant -> {
                          boolean match = true;
                          if (fullyQualified) {
                            match = variant.getName().equals(last);
                          }
                          match &= variant.getPath().equals(first);
                          return match;
                        })
                    .findFirst();

            variantDepOptional.ifPresent(target -> getTargetDeps().add(target));
          }
        });
  }
}
