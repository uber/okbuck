package com.uber.okbuck;

import com.facebook.infer.annotation.Initializer;
import com.google.common.collect.Sets;
import com.uber.okbuck.core.annotation.AnnotationProcessorCache;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.manager.BuckFileManager;
import com.uber.okbuck.core.manager.BuckManager;
import com.uber.okbuck.core.manager.DependencyManager;
import com.uber.okbuck.core.manager.GroovyManager;
import com.uber.okbuck.core.manager.JetifierManager;
import com.uber.okbuck.core.manager.KotlinManager;
import com.uber.okbuck.core.manager.LintManager;
import com.uber.okbuck.core.manager.ManifestMergerManager;
import com.uber.okbuck.core.manager.RobolectricManager;
import com.uber.okbuck.core.manager.ScalaManager;
import com.uber.okbuck.core.manager.TransformManager;
import com.uber.okbuck.core.model.base.ProjectType;
import com.uber.okbuck.core.task.OkBuckCleanTask;
import com.uber.okbuck.core.task.OkBuckTask;
import com.uber.okbuck.core.util.D8Util;
import com.uber.okbuck.core.util.FileUtil;
import com.uber.okbuck.core.util.MoreCollectors;
import com.uber.okbuck.core.util.ProjectCache;
import com.uber.okbuck.core.util.ProjectUtil;
import com.uber.okbuck.extension.KotlinExtension;
import com.uber.okbuck.extension.OkBuckExtension;
import com.uber.okbuck.extension.ScalaExtension;
import com.uber.okbuck.extension.WrapperExtension;
import com.uber.okbuck.generator.BuckFileGenerator;
import com.uber.okbuck.template.common.ExportFile;
import com.uber.okbuck.template.core.Rule;
import com.uber.okbuck.wrapper.BuckWrapperTask;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
  private static final String DOT_OKBUCK = "." + OKBUCK;
  public static final String WORKSPACE_PATH = DOT_OKBUCK + "/workspace";
  public static final String GROUP = OKBUCK;
  public static final String BUCK_LINT = "buckLint";

  private static final String OKBUCK_TARGETS_BZL = "okbuck_targets.bzl";
  public static final String OKBUCK_TARGETS_FILE = DOT_OKBUCK + "/defs/" + OKBUCK_TARGETS_BZL;
  public static final String OKBUCK_TARGETS_TARGET =
      "//" + DOT_OKBUCK + "/defs:" + OKBUCK_TARGETS_BZL;

  private static final String OKBUCK_PREBUILT_BZL = "okbuck_prebuilt.bzl";
  public static final String OKBUCK_PREBUILT_FILE = DOT_OKBUCK + "/defs/" + OKBUCK_PREBUILT_BZL;
  public static final String OKBUCK_PREBUILT_TARGET =
      "//" + DOT_OKBUCK + "/defs:" + OKBUCK_PREBUILT_BZL;

  public static final String OKBUCK_CONFIG = DOT_OKBUCK + "/config";

  private static final String OKBUCK_STATE_DIR = DOT_OKBUCK + "/state";
  private static final String OKBUCK_CLEAN = "okbuckClean";
  private static final String BUCK_WRAPPER = "buckWrapper";
  private static final String FORCED_OKBUCK = "forcedOkbuck";
  private static final String PROCESSOR_BUCK_FILE = WORKSPACE_PATH + "/processor/BUCK";
  private static final String LINT_BUCK_FILE = WORKSPACE_PATH + "/lint/BUCK";

  public static final String OKBUCK_STATE = OKBUCK_STATE_DIR + "/STATE";

  public final Set<String> exportedPaths = Sets.newConcurrentHashSet();

  public DependencyCache depCache;
  public AnnotationProcessorCache annotationProcessorCache;
  public DependencyManager dependencyManager;
  public LintManager lintManager;
  public KotlinManager kotlinManager;
  public ScalaManager scalaManager;
  public GroovyManager groovyManager;
  public JetifierManager jetifierManager;
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

          // Create buck file manager.
          BuckFileManager buckFileManager =
              new BuckFileManager(okbuckExt.getRuleOverridesExtension());

          // Create Annotation Processor cache
          annotationProcessorCache =
              new AnnotationProcessorCache(rootBuckProject, buckFileManager, PROCESSOR_BUCK_FILE);

          // Create Dependency manager
          dependencyManager = new DependencyManager(rootBuckProject, okbuckExt, buckFileManager);

          // Create Lint Manager
          lintManager = new LintManager(rootBuckProject, LINT_BUCK_FILE, buckFileManager);

          // Create Kotlin Manager
          kotlinManager = new KotlinManager(rootBuckProject, buckFileManager);

          // Create Scala Manager
          scalaManager = new ScalaManager(rootBuckProject, buckFileManager);

          // Create Scala Manager
          groovyManager = new GroovyManager(rootBuckProject, buckFileManager);

          // Create Jetifier Manager
          jetifierManager = new JetifierManager(rootBuckProject, buckFileManager, okbuckExt);

          // Create Robolectric Manager
          robolectricManager = new RobolectricManager(rootBuckProject, buckFileManager);

          // Create Transform Manager
          transformManager = new TransformManager(rootBuckProject, buckFileManager);

          // Create Buck Manager
          buckManager = new BuckManager(rootBuckProject);

          // Create Manifest Merger Manager
          manifestMergerManager = new ManifestMergerManager(rootBuckProject, buckFileManager);

          KotlinExtension kotlin = okbuckExt.getKotlinExtension();
          ScalaExtension scala = okbuckExt.getScalaExtension();

          Task rootOkBuckTask =
              rootBuckProject
                  .getTasks()
                  .create(OKBUCK, OkBuckTask.class, okbuckExt, kotlin, scala, buckFileManager);
          rootOkBuckTask.dependsOn(setupOkbuck);
          rootOkBuckTask.doLast(
              task -> {
                annotationProcessorCache.finalizeProcessors();
                dependencyManager.finalizeDependencies();
                jetifierManager.finalizeDependencies();
                lintManager.finalizeDependencies();
                kotlinManager.finalizeDependencies();
                scalaManager.finalizeDependencies();
                groovyManager.finalizeDependencies();
                robolectricManager.finalizeDependencies();
                transformManager.finalizeDependencies();
                buckManager.finalizeDependencies();
                manifestMergerManager.finalizeDependencies();
                writeExportedFileRules(rootBuckProject, okbuckExt);

                // Reset root project's scope cache at the very end
                ProjectCache.resetScopeCache(rootProject);

                // Reset all project's target cache at the very end.
                // This cannot be done for a project just after its okbuck task since,
                // the target cache is accessed by other projects and have to
                // be available until okbuck tasks of all the projects finishes.
                ProjectCache.resetTargetCacheForAll(rootProject);
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

          // Add elements from extraDepCaches to extraDepCachesMap for backwards compatibility
          okbuckExt.extraDepCaches.forEach(
              depCache -> {
                if (!okbuckExt.extraDepCachesMap.containsKey(depCache)) {
                  okbuckExt.extraDepCachesMap.put(depCache, false);
                }
              });

          Map<String, Configuration> extraConfigurations =
              okbuckExt
                  .extraDepCachesMap
                  .keySet()
                  .stream()
                  .collect(
                      Collectors.toMap(
                          Function.identity(),
                          cacheName ->
                              rootBuckProject
                                  .getConfigurations()
                                  .maybeCreate(cacheName + "ExtraDepCache")));

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
                // Init all project's target cache at the very start since a project
                // can access other project's target cache. Hence, all target cache
                // needs to be initialized before any okbuck task starts.
                ProjectCache.initTargetCacheForAll(rootProject);

                // Init root project's scope cache.
                ProjectCache.initScopeCache(rootProject);

                depCache = new DependencyCache(rootBuckProject, dependencyManager, FORCED_OKBUCK);

                // Fetch Lint deps if needed
                if (!okbuckExt.getLintExtension().disabled
                    && okbuckExt.getLintExtension().version != null) {
                  lintManager.fetchLintDeps(okbuckExt.getLintExtension().version);
                }

                // Fetch transform deps if needed
                if (!okbuckExt.getTransformExtension().transforms.isEmpty()) {
                  transformManager.fetchTransformDeps();
                }

                // Setup d8 deps
                D8Util.copyDeps(buckFileManager);

                // Fetch robolectric deps if needed
                if (okbuckExt.getTestExtension().robolectric) {
                  robolectricManager.download();
                }

                if (JetifierManager.isJetifierEnabled(rootProject)) {
                  jetifierManager.setupJetifier(okbuckExt.getJetifierExtension().version);
                }

                extraConfigurations.forEach(
                    (cacheName, extraConfiguration) ->
                        new DependencyCache(
                                rootBuckProject,
                                dependencyManager,
                                okbuckExt.extraDepCachesMap.getOrDefault(cacheName, false))
                            .build(extraConfiguration));

                buckManager.setupBuckBinary();

                manifestMergerManager.fetchManifestMergerDeps();
              });

          // Create clean task
          Task okBuckClean =
              rootBuckProject
                  .getTasks()
                  .create(OKBUCK_CLEAN, OkBuckCleanTask.class, okbuckExt.buckProjects);
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
                        task -> {
                          ProjectCache.initScopeCache(bp);
                          BuckFileGenerator.generate(
                              bp, buckFileManager, okbuckExt.getVisibilityExtension());
                          ProjectCache.resetScopeCache(bp);
                        });
                    okbuckProjectTask.dependsOn(setupOkbuck);
                    okBuckClean.dependsOn(okbuckProjectTask);
                  });
        });
  }

  private void writeExportedFileRules(Project rootBuckProject, OkBuckExtension okBuckExtension) {
    Set<String> currentProjectPaths =
        okBuckExtension
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
    Map<String, Set<Rule>> pathToRules = new HashMap<>();
    for (String exportedPath : exportedPaths) {
      File exportedFile = rootBuckProject.file(exportedPath);
      String containingPath =
          FileUtil.getRelativePath(rootBuckProject.getProjectDir(), exportedFile.getParentFile());
      Set<Rule> rules = pathToRules.getOrDefault(containingPath, new HashSet<>());
      rules.add(new ExportFile().name(exportedFile.getName()));
      pathToRules.put(containingPath, rules);
    }
    for (Map.Entry<String, Set<Rule>> entry : pathToRules.entrySet()) {
      File buckFile =
          rootBuckProject.getRootDir().toPath().resolve(entry.getKey()).resolve(BUCK).toFile();
      try (OutputStream os =
          new FileOutputStream(buckFile, currentProjectPaths.contains(entry.getKey()))) {
        entry
            .getValue()
            .stream()
            .sorted((rule1, rule2) -> rule1.name().compareToIgnoreCase(rule2.name()))
            .forEach(rule -> rule.render(os));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
