package com.uber.okbuck.generator;

import com.uber.okbuck.extension.OkBuckExtension;
import com.uber.okbuck.template.config.BuckConfig;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public final class BuckConfigLocalGenerator {

  private BuckConfigLocalGenerator() {}

  /** generate {@link BuckConfig} */
  public static BuckConfig generate(
      OkBuckExtension okbuck,
      @Nullable String groovyHome,
      @Nullable String kotlinHome,
      @Nullable String scalaCompiler,
      @Nullable String scalaLibrary,
      @Nullable String proguardJar,
      Set<String> defs) {

    return new BuckConfig()
        .buildToolsVersion(okbuck.buildToolVersion)
        .target(okbuck.target)
        .groovyHome(groovyHome)
        .kotlinHome(kotlinHome)
        .scalaCompiler(scalaCompiler)
        .scalaLibrary(scalaLibrary)
        .proguardJar(proguardJar)
        .defs(defs);
  }
}
