package com.uber.okbuck.core.model.android;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Var;
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
  @Nullable private final String manifest;

  ExoPackageScope(
      Project project, Scope base, List<String> exoPackageDependencies, @Nullable String manifest) {
    super(project, null, ImmutableSet.of(), ImmutableSet.of(), base.getCustomOptions());
    this.base = base;
    this.manifest = manifest;
    extractDependencies(base, exoPackageDependencies);
  }

  @Nullable
  public String getAppClass() {
    if (manifest == null) {
      return null;
    }
    @Var String appClass = null;

    File manifestFile = project.file(manifest);
    Document manifestXml = XmlUtil.loadXml(manifestFile);
    try {
      NodeList nodeList = manifestXml.getElementsByTagName("application");
      Preconditions.checkArgument(nodeList.getLength() == 1);

      Element application = (Element) nodeList.item(0);

      appClass =
          application.getAttribute("android:name").replaceAll("\\.", "/").replaceAll("^/", "");
    } catch (Exception ignored) {
    }

    String finalAppClass = appClass;

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
          boolean qualified;

          if (exoPackageDep.contains(":")) {
            List<String> parts = Splitter.on(':').splitToList(exoPackageDep);
            first = parts.get(0);
            last = parts.get(1);
            qualified = true;
          } else {
            first = last = exoPackageDep;
            qualified = false;
          }

          Optional<ExternalDependency> externalDepOptional =
              base.getExternalDeps(false)
                  .stream()
                  .filter(
                      dependency -> {
                        if (qualified) {
                          return dependency.getGroup().equals(first)
                              && dependency.getName().equals(last);
                        } else {
                          return dependency.getName().equals(last);
                        }
                      })
                  .findFirst();

          if (externalDepOptional.isPresent()) {
            this.external.add(externalDepOptional.get());
          } else {
            Optional<Target> variantDepOptional =
                base.getTargetDeps(false)
                    .stream()
                    .filter(
                        variant -> {
                          if (qualified) {
                            return variant.getName().equals(last)
                                && variant.getPath().equals(first);
                          } else {
                            return variant.getPath().equals(first);
                          }
                        })
                    .findFirst();

            variantDepOptional.ifPresent(targetDeps::add);
          }
        });
  }
}
