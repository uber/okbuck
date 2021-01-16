package com.uber.okbuck.core.manager;

import com.google.common.collect.ImmutableList;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.model.base.RuleType;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.extension.KotlinExtension;
import com.uber.okbuck.extension.OkBuckExtension;
import com.uber.okbuck.template.common.Genrule;
import com.uber.okbuck.template.common.HttpArchive;
import com.uber.okbuck.template.core.Rule;
import com.uber.okbuck.template.java.NativePrebuilt;
import java.nio.file.Path;
import javax.annotation.Nullable;
import org.gradle.api.Project;

public final class KotlinManager {
  public static final String KOTLIN_ANDROID_EXTENSIONS_MODULE = "kotlin-android-extensions";
  public static final String KOTLIN_ALLOPEN_MODULE = "kotlin-allopen";

  public static final String KOTLIN_HOME = "kotlin_home";
  public static final String KOTLIN_HOME_LOCATION =
      OkBuckGradlePlugin.WORKSPACE_PATH + "/" + KOTLIN_HOME;
  public static final String KOTLIN_HOME_TARGET = "//" + KOTLIN_HOME_LOCATION + ":" + KOTLIN_HOME;
  public static final String KOTLIN_KAPT_PLUGIN = "kotlin-kapt";

  private static final String KOTLIN_AE_NAME = "android-extensions-compiler";
  public static final String KOTLIN_AE_PLUGIN_TARGET =
      "//" + KOTLIN_HOME_LOCATION + ":" + KOTLIN_AE_NAME + ".jar";

  private static final String KOTLIN_AO_NAME = "allopen-compiler-plugin";
  public static final String KOTLIN_AO_PLUGIN_TARGET =
      "//" + KOTLIN_HOME_LOCATION + ":" + KOTLIN_AO_NAME + ".jar";

  public static final String COPY_COMMAND = "cp $(location :kotlin_home)/lib/%s.jar $OUT";

  private final Project project;
  private final BuckFileManager buckFileManager;

  private boolean kotlinHomeEnabled;
  @Nullable private KotlinExtension kotlinExtension;

  public KotlinManager(Project project, BuckFileManager buckFileManager) {
    this.project = project;
    this.buckFileManager = buckFileManager;
  }

  public void setupKotlinHome(KotlinExtension kotlinExtension) {
    this.kotlinHomeEnabled = true;
    this.kotlinExtension = kotlinExtension;
  }

  public void finalizeDependencies(OkBuckExtension okBuckExtension) {
    Path path = project.file(KOTLIN_HOME_LOCATION).toPath();
    FileUtil.deleteQuietly(path);

    if (!kotlinHomeEnabled) {
      // no-op if kotlin home is not enabled
      return;
    }

    if (kotlinExtension == null || kotlinExtension.version == null) {
      throw new IllegalStateException("kotlinVersion is not setup");
    }

    Rule downloadRule =
        new HttpArchive()
            .sha256(kotlinExtension.getCompilerZipSha256())
            .urls(ImmutableList.of(kotlinExtension.getCompilerZipDownloadUrl()))
            .stripPrefix("kotlinc")
            .type("zip")
            .name(KOTLIN_HOME);

    ImmutableList<Rule> rules =
        new ImmutableList.Builder<Rule>()
            .add(downloadRule)
            .addAll(getRules(KOTLIN_AE_NAME))
            .addAll(getRules(KOTLIN_AO_NAME))
            .build();

    buckFileManager.writeToBuckFile(rules, path.resolve(okBuckExtension.buildFileName).toFile());
  }

  private static ImmutableList<Rule> getRules(String name) {
    return ImmutableList.of(
        new Genrule()
            .cmd(String.format(COPY_COMMAND, name))
            .out(name + "__copy.jar")
            .name(name + "__copy"),
        new NativePrebuilt()
            .prebuiltType(RuleType.PREBUILT_JAR.getProperties().get(0))
            .prebuilt(":" + name + "__copy")
            .ruleType(RuleType.PREBUILT_JAR.getBuckName())
            .name(name + ".jar"));
  }
}
