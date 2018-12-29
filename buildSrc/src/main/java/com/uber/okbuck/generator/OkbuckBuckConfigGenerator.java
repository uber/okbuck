package com.uber.okbuck.generator;

import com.uber.okbuck.extension.OkBuckExtension;
import com.uber.okbuck.template.config.BuckConfig;
import java.util.LinkedHashMap;
import javax.annotation.Nullable;

public final class OkbuckBuckConfigGenerator {

  private OkbuckBuckConfigGenerator() {}

  /** generate {@link BuckConfig} */
  public static BuckConfig generate(
      OkBuckExtension okbuck,
      @Nullable String groovyHome,
      @Nullable String kotlinHome,
      @Nullable String scalaCompiler,
      @Nullable String scalaLibrary,
      @Nullable String proguardJar,
      LinkedHashMap<String, String> repositories) {

    return new BuckConfig()
        .buildToolsVersion(okbuck.buildToolVersion)
        .target(okbuck.target)
        .groovyHome(groovyHome)
        .kotlinHome(kotlinHome)
        .scalaCompiler(scalaCompiler)
        .scalaLibrary(scalaLibrary)
        .proguardJar(proguardJar)
        .mavenRepositories(repositories);
  }
}
