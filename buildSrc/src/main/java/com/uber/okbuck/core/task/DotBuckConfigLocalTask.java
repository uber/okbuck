package com.uber.okbuck.core.task;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.config.DotBuckConfigLocalFile;
import com.uber.okbuck.extension.OkBuckExtension;
import com.uber.okbuck.generator.DotBuckConfigLocalGenerator;

import org.gradle.api.internal.AbstractTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Set;

public class DotBuckConfigLocalTask extends AbstractTask {
  OkBuckExtension okbuck;

  String groovyHome;

  String kotlinCompiler;

  String kotlinRuntime;

  String proguardJar;

  Set<String> defs;

  public DotBuckConfigLocalTask() {
  }

  @TaskAction
  public void generateDotBuckConfigTask() {
    System.out.println("generating dotbuckconfig");
    DotBuckConfigLocalFile dotBuckConfigLocalFile = DotBuckConfigLocalGenerator.generate(okbuck, groovyHome,
            kotlinCompiler, kotlinRuntime, proguardJar, defs);

    // generate .buckconfig.local
    try (PrintStream configPrinter = new PrintStream(dotBuckConfigLocal())) {
      dotBuckConfigLocalFile.print(configPrinter);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override public String getGroup() {
    return OkBuckGradlePlugin.GROUP;
  }

  @Override public String getDescription() {
    return "Generate .buckconfig";
  }

  @Nested
  public OkBuckExtension getOkbuck() {
    return okbuck;
  }

  @Input
  public String getGroovyHome() {
    return groovyHome;
  }

  @Input
  public String getKotlinCompiler() {
    return kotlinCompiler;
  }

  @Input
  public String getKotlinRuntime() {
    return kotlinRuntime;
  }

  @Input
  public String getProguardJar() {
    return proguardJar;
  }

  @Input
  public Set<String> getDefs() {
    return defs;
  }

  @OutputFile
  public File dotBuckConfigLocal() {
    return getProject().file(".buckconfig.local");
  }
}
