package com.uber.okbuck;

import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.DependencyUtils;
import com.uber.okbuck.core.model.base.AnnotationProcessorCache;
import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.core.model.base.TargetCache;
import com.uber.okbuck.core.task.OkBuckCleanTask;
import com.uber.okbuck.core.task.OkBuckTask;
import com.uber.okbuck.core.util.D8Util;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.LintUtil;
import com.uber.okbuck.core.util.RobolectricUtil;
import com.uber.okbuck.core.util.TransformUtil;
import com.uber.okbuck.extension.KotlinExtension;
import com.uber.okbuck.extension.OkBuckExtension;
import com.uber.okbuck.extension.ScalaExtension;
import com.uber.okbuck.extension.WrapperExtension;
import com.uber.okbuck.generator.BuckFileGenerator;
import com.uber.okbuck.wrapper.BuckWrapperTask;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.jetbrains.annotations.NotNull;

public class OkBuckGradlePlugin implements Plugin<Project> {
  public static final String BUCK = "BUCK";
  public static final String OKBUCK = "okbuck";
  public static final String DEFAULT_CACHE_PATH = ".okbuck/cache";
  public static final String GROUP = "okbuck";
  public static final String BUCK_LINT = "buckLint";
  public static final String OKBUCK_DEFS = ".okbuck/defs/DEFS";
  public static final String OKBUCK_STATE_DIR = ".okbuck/state";
  public static final String OKBUCK_STATE = OKBUCK_STATE_DIR + "/STATE";
  public static final String OKBUCK_GEN = ".okbuck/gen";

  private static final String EXTERNAL_DEP_BUCK_FILE = "thirdparty/BUCK_FILE";
  private static final String OKBUCK_CLEAN = "okbuckClean";
  private static final String BUCK_WRAPPER = "buckWrapper";
  private static final String EXTRA_DEP_CACHE_PATH = ".okbuck/cache/extra";
  private static final String FORCED_OKBUCK = "forcedOkbuck";
  private static final String BUCK_BINARY = "buck_binary";
  private static final String JITPACK_URL = "https://jitpack.io";
  private static final String BUCK_BINARY_CONFIGURATION = "buckBinary";
  private static final String PROCESSOR_BUCK_FILE = ".okbuck/cache/processor/BUCK";

  public final Map<Project, Map<String, Scope>> scopes = new ConcurrentHashMap<>();

  public DependencyCache depCache;
  public DependencyCache lintDepCache;
  public TargetCache targetCache;
  public AnnotationProcessorCache annotationProcessorCache;

  public void apply(@NotNull Project project) {
    // Create extensions
    OkBuckExtension okbuckExt =
        project.getExtensions().create(OKBUCK, OkBuckExtension.class, project);

    // Create configurations
    project.getConfigurations().maybeCreate(TransformUtil.CONFIGURATION_TRANSFORM);
    project.getConfigurations().maybeCreate(FORCED_OKBUCK);
    Configuration buckBinaryConfiguration =
        project.getConfigurations().maybeCreate(BUCK_BINARY_CONFIGURATION);

    project.afterEvaluate(
        buckProject -> {
          // Create tasks
          Task setupOkbuck = project.getTasks().create("setupOkbuck");
          setupOkbuck.setGroup(GROUP);
          setupOkbuck.setDescription("Setup okbuck cache and dependencies");

          KotlinExtension kotlin = okbuckExt.getKotlinExtension();
          ScalaExtension scala = okbuckExt.getScalaExtension();

          Task okBuck =
              project.getTasks().create(OKBUCK, OkBuckTask.class, okbuckExt, kotlin, scala);
          okBuck.dependsOn(setupOkbuck);
          okBuck.doLast(
              task -> {
                annotationProcessorCache.finalizeProcessors();
                depCache.finalizeDeps();
              });

          // Create target cache
          targetCache = new TargetCache();

          // Create Annotation Processor cache
          annotationProcessorCache =
              new AnnotationProcessorCache(project.getRootProject(), PROCESSOR_BUCK_FILE);

          WrapperExtension wrapper = okbuckExt.getWrapperExtension();
          // Create wrapper task
          buckProject
              .getTasks()
              .create(
                  BUCK_WRAPPER,
                  BuckWrapperTask.class,
                  wrapper.repo,
                  wrapper.watch,
                  wrapper.sourceRoots,
                  wrapper.ignoredDirs);

          Map<String, Configuration> extraConfigurations =
              okbuckExt
                  .extraDepCaches
                  .stream()
                  .collect(
                      Collectors.toMap(
                          Function.identity(),
                          cacheName ->
                              buckProject
                                  .getConfigurations()
                                  .maybeCreate(cacheName + "ExtraDepCache")));

          // Create dependency cache for buck binary if needed
          if (okbuckExt.buckBinary != null) {
            buckProject
                .getRepositories()
                .maven(mavenArtifactRepository -> mavenArtifactRepository.setUrl(JITPACK_URL));
            buckProject.getDependencies().add(BUCK_BINARY_CONFIGURATION, okbuckExt.buckBinary);
          }

          setupOkbuck.doFirst(
              task -> {
                if (System.getProperty("okbuck.wrapper", "false").equals("false")) {
                  throw new IllegalArgumentException(
                      "Okbuck cannot be invoked without 'okbuck.wrapper' set to true. Use buckw instead");
                }
              });

          // Configure setup task
          setupOkbuck.doLast(
              task -> {
                // Cleanup gen folder
                FileUtil.deleteQuietly(buckProject.getProjectDir().toPath().resolve(OKBUCK_GEN));

                okbuckExt.buckProjects.forEach(p -> targetCache.getTargets(p));

                File cacheDir =
                    DependencyUtils.createCacheDir(
                        buckProject, DEFAULT_CACHE_PATH, EXTERNAL_DEP_BUCK_FILE);
                depCache = new DependencyCache(buckProject, cacheDir, FORCED_OKBUCK);

                // Fetch Lint deps if needed
                if (!okbuckExt.getLintExtension().disabled
                    && okbuckExt.getLintExtension().version != null) {
                  LintUtil.fetchLintDeps(buckProject, okbuckExt.getLintExtension().version);
                }

                // Fetch transform deps if needed
                if (okbuckExt.getExperimentalExtension().transform) {
                  TransformUtil.fetchTransformDeps(buckProject);
                }

                // Setup d8 deps
                D8Util.copyDeps();

                // Fetch robolectric deps if needed
                if (okbuckExt.getTestExtension().robolectric) {
                  RobolectricUtil.download(buckProject);
                }

                extraConfigurations.forEach(
                    (cacheName, extraConfiguration) ->
                        new DependencyCache(
                                buckProject,
                                DependencyUtils.createCacheDir(
                                    buckProject,
                                    EXTRA_DEP_CACHE_PATH + "/" + cacheName,
                                    EXTERNAL_DEP_BUCK_FILE))
                            .build(extraConfiguration));

                // Fetch buck binary
                new DependencyCache(
                        buckProject,
                        DependencyUtils.createCacheDir(
                            buckProject, DEFAULT_CACHE_PATH + "/" + BUCK_BINARY))
                    .build(buckBinaryConfiguration);
              });

          // Create clean task
          Task okBuckClean =
              buckProject
                  .getTasks()
                  .create(
                      OKBUCK_CLEAN,
                      OkBuckCleanTask.class,
                      okbuckExt.buckProjects,
                      PROCESSOR_BUCK_FILE);
          okBuck.dependsOn(okBuckClean);

          // Configure buck projects
          okbuckExt
              .buckProjects
              .stream()
              .filter(p -> p.getBuildFile().exists())
              .forEach(
                  bp -> {
                    bp.getConfigurations().maybeCreate(BUCK_LINT);
                    Task okbuckProjectTask = bp.getTasks().maybeCreate(OKBUCK);
                    okbuckProjectTask.doLast(task -> BuckFileGenerator.generate(bp));
                    okbuckProjectTask.dependsOn(setupOkbuck);
                    okBuckClean.dependsOn(okbuckProjectTask);
                  });
        });
  }
}
