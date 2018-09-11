package com.uber.okbuck;

import com.facebook.infer.annotation.Initializer;
import com.google.common.collect.Sets;
import com.uber.okbuck.core.annotation.AnnotationProcessorCache;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.manager.BuckManager;
import com.uber.okbuck.core.manager.DependencyManager;
import com.uber.okbuck.core.manager.GroovyManager;
import com.uber.okbuck.core.manager.KotlinManager;
import com.uber.okbuck.core.manager.LintManager;
import com.uber.okbuck.core.manager.ManifestMergerManager;
import com.uber.okbuck.core.manager.RobolectricManager;
import com.uber.okbuck.core.manager.ScalaManager;
import com.uber.okbuck.core.manager.TransformManager;
import com.uber.okbuck.core.model.base.ProjectType;
import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.core.model.base.TargetCache;
import com.uber.okbuck.core.task.OkBuckCleanTask;
import com.uber.okbuck.core.task.OkBuckTask;
import com.uber.okbuck.core.util.D8Util;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.MoreCollectors;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.KotlinExtension;
import com.uber.okbuck.extension.OkBuckExtension;
import com.uber.okbuck.extension.ScalaExtension;
import com.uber.okbuck.extension.WrapperExtension;
import com.uber.okbuck.generator.BuckFileGenerator;
import com.uber.okbuck.template.common.ExportFile;
import com.uber.okbuck.wrapper.BuckWrapperTask;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;

// Dependency Tree
//
//                 rootOkBuckTask
//            /               \
//           /                v
//          /             okbuckClean
//         /        /          |          \
//        /        v           v          v
//       /     :p1:okbuck   :p2:okbuck   :p3:okbuck     ...
//      |        /           /               /
//      v       v           v               v
//                setupOkbuck
//

public class OkBuckGradlePlugin implements Plugin<Project> {
  public static final String BUCK = "BUCK";
  public static final String OKBUCK = "okbuck";
  public static final String WORKSPACE_PATH = ".okbuck/workspace";
  public static final String GROUP = "okbuck";
  public static final String BUCK_LINT = "buckLint";
  public static final String OKBUCK_DEFS = ".okbuck/defs/DEFS";
  public static final String OKBUCK_GEN = ".okbuck/gen";
  public static final String OKBUCK_CONFIG = ".okbuck/config";

  private static final String OKBUCK_STATE_DIR = ".okbuck/state";
  private static final String OKBUCK_CLEAN = "okbuckClean";
  private static final String BUCK_WRAPPER = "buckWrapper";
  private static final String FORCED_OKBUCK = "forcedOkbuck";
  private static final String PROCESSOR_BUCK_FILE = WORKSPACE_PATH + "/processor/BUCK";
  private static final String LINT_BUCK_FILE = WORKSPACE_PATH + "/lint/BUCK";

  public static final String OKBUCK_STATE = OKBUCK_STATE_DIR + "/STATE";
  public final Map<Project, Map<String, Scope>> scopes = new ConcurrentHashMap<>();
  public final Set<String> keystores = Sets.newConcurrentHashSet();

  public DependencyCache depCache;
  public DependencyCache lintDepCache;
  public TargetCache targetCache;
  public AnnotationProcessorCache annotationProcessorCache;
  public DependencyManager dependencyManager;
  public LintManager lintManager;
  public KotlinManager kotlinManager;
  public ScalaManager scalaManager;
  public GroovyManager groovyManager;
  public TransformManager transformManager;

  ManifestMergerManager manifestMergerManager;
  RobolectricManager robolectricManager;
  BuckManager buckManager;

  // Only apply to the root project
  @Initializer
  @SuppressWarnings("NullAway")
  @Override
  public void apply(Project rootProject) {
    // Create extensions
    OkBuckExtension okbuckExt =
        rootProject.getExtensions().create(OKBUCK, OkBuckExtension.class, rootProject);

    // Create configurations
    rootProject.getConfigurations().maybeCreate(TransformManager.CONFIGURATION_TRANSFORM);
    rootProject.getConfigurations().maybeCreate(FORCED_OKBUCK);

    rootProject.afterEvaluate(
        rootBuckProject -> {
          // Create tasks
          Task setupOkbuck = rootBuckProject.getTasks().create("setupOkbuck");
          setupOkbuck.setGroup(GROUP);
          setupOkbuck.setDescription("Setup okbuck cache and dependencies");

          // Create target cache
          targetCache = new TargetCache();

          // Create Annotation Processor cache
          annotationProcessorCache =
              new AnnotationProcessorCache(rootBuckProject, PROCESSOR_BUCK_FILE);

          // Create Dependency manager
          dependencyManager =
              new DependencyManager(
                  rootBuckProject,
                  okbuckExt.externalDependencyCache,
                  okbuckExt.getExternalDependenciesExtension());

          // Create Lint Manager
          lintManager = new LintManager(rootBuckProject, LINT_BUCK_FILE);

          // Create Kotlin Manager
          kotlinManager = new KotlinManager(rootBuckProject, okbuckExt);

          // Create Scala Manager
          scalaManager = new ScalaManager(rootBuckProject);

          // Create Scala Manager
          groovyManager = new GroovyManager(rootBuckProject);

          // Create Robolectric Manager
          robolectricManager = new RobolectricManager(rootBuckProject);

          // Create Transform Manager
          transformManager = new TransformManager(rootBuckProject);

          // Create Buck Manager
          buckManager = new BuckManager(rootBuckProject);

          // Create Manifest Merger Manager
          manifestMergerManager = new ManifestMergerManager(rootBuckProject);

          KotlinExtension kotlin = okbuckExt.getKotlinExtension();
          ScalaExtension scala = okbuckExt.getScalaExtension();

          Task rootOkBuckTask =
              rootBuckProject.getTasks().create(OKBUCK, OkBuckTask.class, okbuckExt, kotlin, scala);
          rootOkBuckTask.dependsOn(setupOkbuck);
          rootOkBuckTask.doLast(
              task -> {
                annotationProcessorCache.finalizeProcessors();
                dependencyManager.finalizeDependencies();
                lintManager.finalizeDependencies();
                kotlinManager.finalizeDependencies();
                scalaManager.finalizeDependencies();
                groovyManager.finalDependencies();
                robolectricManager.finalizeDependencies();
                transformManager.finalizeDependencies();
                buckManager.finalizeDependencies();
                manifestMergerManager.finalizeDependencies();
              });

          WrapperExtension wrapper = okbuckExt.getWrapperExtension();
          // Create wrapper task
          rootBuckProject
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
                              rootBuckProject
                                  .getConfigurations()
                                  .maybeCreate(cacheName + "ExtraDepCache")));

          Set<String> currentProjectPaths =
              okbuckExt
                  .buckProjects
                  .stream()
                  .filter(project -> ProjectUtil.getType(project) != ProjectType.UNKNOWN)
                  .map(
                      project ->
                          rootBuckProject
                              .getProjectDir()
                              .toPath()
                              .relativize(project.getProjectDir().toPath())
                              .toString())
                  .collect(MoreCollectors.toImmutableSet());

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
                FileUtil.deleteQuietly(
                    rootBuckProject.getProjectDir().toPath().resolve(OKBUCK_GEN));

                okbuckExt.buckProjects.forEach(p -> targetCache.getTargets(p));

                depCache = new DependencyCache(rootBuckProject, dependencyManager, FORCED_OKBUCK);

                // Fetch Lint deps if needed
                if (!okbuckExt.getLintExtension().disabled
                    && okbuckExt.getLintExtension().version != null) {
                  lintManager.fetchLintDeps(okbuckExt.getLintExtension().version);
                }

                // Fetch transform deps if needed
                if (okbuckExt.getExperimentalExtension().transform) {
                  transformManager.fetchTransformDeps();
                }

                // Setup d8 deps
                D8Util.copyDeps();

                // Fetch robolectric deps if needed
                if (okbuckExt.getTestExtension().robolectric) {
                  robolectricManager.download();
                }

                extraConfigurations.forEach(
                    (cacheName, extraConfiguration) ->
                        new DependencyCache(rootBuckProject, dependencyManager)
                            .build(extraConfiguration));

                buckManager.setupBuckBinary();

                manifestMergerManager.fetchManifestMergerDeps();

                // Write out keystore rules
                for (String keystore : keystores) {
                  File keystoreFile = rootBuckProject.file(keystore);
                  File containingDir = keystoreFile.getParentFile();
                  File buckFile = new File(containingDir, BUCK);
                  try (OutputStream os =
                      new FileOutputStream(
                          buckFile, currentProjectPaths.contains(containingDir.toString()))) {
                    new ExportFile().name(keystoreFile.getName()).render(os);
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                }
              });

          // Create clean task
          Task okBuckClean =
              rootBuckProject
                  .getTasks()
                  .create(OKBUCK_CLEAN, OkBuckCleanTask.class, currentProjectPaths);
          rootOkBuckTask.dependsOn(okBuckClean);

          // Create okbuck task on each project to generate their buck file
          okbuckExt
              .buckProjects
              .stream()
              .filter(p -> p.getBuildFile().exists())
              .forEach(
                  bp -> {
                    bp.getConfigurations().maybeCreate(BUCK_LINT);
                    Task okbuckProjectTask = bp.getTasks().maybeCreate(OKBUCK);
                    okbuckProjectTask.doLast(
                        task -> BuckFileGenerator.generate(bp, okbuckExt.getVisibilityExtension()));
                    okbuckProjectTask.dependsOn(setupOkbuck);
                    okBuckClean.dependsOn(okbuckProjectTask);
                  });
        });
  }
}
