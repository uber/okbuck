package com.uber.okbuck.core.task;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.model.base.ProjectType;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.GroovyUtil;
import com.uber.okbuck.core.util.KotlinUtil;
import com.uber.okbuck.core.util.ProguardUtil;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.OkBuckExtension;
import com.uber.okbuck.generator.DotBuckConfigLocalGenerator;

import org.apache.commons.lang3.tuple.Pair;
import org.gradle.api.DefaultTask;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Set;
import java.util.stream.Collectors;

import static com.uber.okbuck.OkBuckGradlePlugin.OKBUCK_DEFS;

public class OkBuckTask extends DefaultTask {

  @Nested public OkBuckExtension okBuckExtension;

  public OkBuckTask() {
    // Never up to date; this task isn't safe to run incrementally.
    getOutputs().upToDateWhen(Specs.satisfyNone());
  }

  @TaskAction
  void okbuck() {
    // Fetch Groovy support deps if needed
    boolean hasGroovyLib = okBuckExtension.buckProjects.parallelStream().anyMatch(project -> ProjectUtil.getType(project) == ProjectType.GROOVY_LIB);
    if (hasGroovyLib) {
      GroovyUtil.setupGroovyHome(getProject());
    }

    // Fetch Kotlin support deps if needed
    boolean hasKotlinLib = okBuckExtension.buckProjects.parallelStream().anyMatch(project -> ProjectUtil.getType(project) == ProjectType.KOTLIN_LIB);
    if (hasKotlinLib) {
      KotlinUtil.setupKotlinHome(getProject());
    }

    generate(okBuckExtension,
            hasGroovyLib ? GroovyUtil.GROOVY_HOME_LOCATION : null,
            hasKotlinLib ? KotlinUtil.KOTLIN_HOME_LOCATION : null);
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

  private void generate(OkBuckExtension okbuckExt, String groovyHome, String kotlinHome) {
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
    try (
    PrintStream configPrinter = new PrintStream(dotBuckConfigLocal())) {
      DotBuckConfigLocalGenerator.generate(okbuckExt,
              groovyHome,
              kotlinHome,
              ProguardUtil.getProguardJarPath(getProject()),
              defs).print(configPrinter);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
