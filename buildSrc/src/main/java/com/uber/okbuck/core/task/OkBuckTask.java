package com.uber.okbuck.core.task;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.config.BUCKFile;
import com.uber.okbuck.core.io.FilePrinter;
import com.uber.okbuck.core.io.Printer;
import com.uber.okbuck.core.model.base.ProjectType;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.GroovyUtil;
import com.uber.okbuck.core.util.KotlinUtil;
import com.uber.okbuck.core.util.ProguardUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.core.util.ScalaUtil;
import com.uber.okbuck.extension.OkBuckExtension;
import com.uber.okbuck.extension.ScalaExtension;
import com.uber.okbuck.generator.DotBuckConfigLocalGenerator;

import org.gradle.api.DefaultTask;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Set;
import java.util.stream.Collectors;

import static com.uber.okbuck.OkBuckGradlePlugin.OKBUCK_DEFS;

public class OkBuckTask extends DefaultTask {

  @Nested public OkBuckExtension okBuckExtension;

  @Nested public ScalaExtension scalaExtension;

  public OkBuckTask() {
    // Never up to date; this task isn't safe to run incrementally.
    getOutputs().upToDateWhen(Specs.satisfyNone());
  }

  @TaskAction
  void okbuck() {
    // Fetch Groovy support deps if needed
    boolean hasGroovyLib = okBuckExtension.buckProjects.parallelStream().anyMatch(
            project -> ProjectUtil.getType(project) == ProjectType.GROOVY_LIB);
    if (hasGroovyLib) {
      GroovyUtil.setupGroovyHome(getProject());
    }

    // Fetch Kotlin support deps if needed
    boolean hasKotlinLib = KotlinUtil.hasKotlinPluginInClasspath(getProject());
    if (hasKotlinLib) {
      KotlinUtil.setupKotlinHome(getProject());
    }

    // Fetch Scala support deps if needed
    boolean hasScalaLib = okBuckExtension.buckProjects.parallelStream().anyMatch(
            project -> ProjectUtil.getType(project) == ProjectType.SCALA_LIB);
    if (hasScalaLib) {
      ScalaUtil.setupScalaHome(getProject(), scalaExtension.version);
    }

    generate(okBuckExtension,
            hasGroovyLib ? GroovyUtil.GROOVY_HOME_LOCATION : null,
            hasKotlinLib ? KotlinUtil.KOTLIN_HOME_LOCATION : null,
            hasScalaLib ? ScalaUtil.SCALA_HOME_LOCATION : null);

    ProjectUtil.getDependencyCache(getProject()).finalizeDeps();
  }

  @Override public String getGroup() {
    return OkBuckGradlePlugin.GROUP;
  }

  @Override public String getDescription() {
    return "Okbuck task for the root project. Also sets up groovy and kotlin if required.";
  }

  @OutputFile
  public File okbuckDefs() {
    return getProject().file(OKBUCK_DEFS);
  }

  @OutputFile
  public File dotBuckConfig() {
    return getProject().file(".buckconfig");
  }

  @OutputFile
  public File dotBuckConfigLocal() {
    return getProject().file(".buckconfig.local");
  }

  private void generate(OkBuckExtension okbuckExt, String groovyHome, String kotlinHome,
          String scalaHome) {
    // generate empty .buckconfig if it does not exist
    if (!dotBuckConfig().exists()) {
      try {
        dotBuckConfig().createNewFile();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    // Setup defs
    FileUtil.copyResourceToProject("defs/OKBUCK_DEFS_TEMPLATE", okbuckDefs(),
            ImmutableMap.of("template-resource-excludes", Joiner.on(", ")
                    .join(okBuckExtension.excludeResources
                            .stream()
                            .map(s -> "'" + s + "'")
                            .collect(Collectors.toSet()))));
    Set<String> defs = okbuckExt.extraDefs.stream()
            .map(it -> "//" + FileUtil.getRelativePath(getProject().getRootDir(), it))
            .collect(Collectors.toSet());
    defs.add("//" + OKBUCK_DEFS);

    // generate .buckconfig.local
    try {
      Printer printer = new FilePrinter(dotBuckConfigLocal());
      DotBuckConfigLocalGenerator.generate(okbuckExt,
              groovyHome,
              kotlinHome,
              scalaHome,
              ProguardUtil.getProguardJarPath(getProject()),
              defs).print(printer);
      printer.flush();
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
