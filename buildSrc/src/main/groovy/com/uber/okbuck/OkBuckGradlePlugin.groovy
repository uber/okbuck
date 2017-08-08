package com.uber.okbuck

import com.uber.okbuck.core.dependency.DependencyCache
import com.uber.okbuck.core.dependency.DependencyUtils
import com.uber.okbuck.core.model.base.Scope
import com.uber.okbuck.core.model.base.TargetCache
import com.uber.okbuck.core.task.OkBuckCleanTask
import com.uber.okbuck.core.task.OkBuckTask
import com.uber.okbuck.core.util.FileUtil
import com.uber.okbuck.core.util.LintUtil
import com.uber.okbuck.core.util.RetrolambdaUtil
import com.uber.okbuck.core.util.RobolectricUtil
import com.uber.okbuck.core.util.TransformUtil
import com.uber.okbuck.extension.ExperimentalExtension
import com.uber.okbuck.extension.IntellijExtension
import com.uber.okbuck.extension.LintExtension
import com.uber.okbuck.extension.OkBuckExtension
import com.uber.okbuck.extension.RetrolambdaExtension
import com.uber.okbuck.extension.ScalaExtension
import com.uber.okbuck.extension.TestExtension
import com.uber.okbuck.extension.TransformExtension
import com.uber.okbuck.extension.WrapperExtension
import com.uber.okbuck.generator.BuckFileGenerator
import com.uber.okbuck.wrapper.BuckWrapperTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration

// Dependency Tree
//
//                 okbuck
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

class OkBuckGradlePlugin implements Plugin<Project> {

    public static final String EXTERNAL_DEP_BUCK_FILE = "thirdparty/BUCK_FILE"
    public static final String OKBUCK = "okbuck"
    public static final String OKBUCK_CLEAN = 'okbuckClean'
    public static final String BUCK = "BUCK"
    public static final String EXPERIMENTAL = "experimental"
    public static final String INTELLIJ = "intellij"
    public static final String TEST = "test"
    public static final String WRAPPER = "wrapper"
    public static final String BUCK_WRAPPER = "buckWrapper"
    public static final String DEFAULT_CACHE_PATH = ".okbuck/cache"
    public static final String EXTRA_DEP_CACHE_PATH = ".okbuck/cache/extra"
    public static final String GROUP = "okbuck"
    public static final String BUCK_LINT = "buckLint"
    public static final String LINT = "lint"
    public static final String TRANSFORM = "transform"
    public static final String RETROLAMBDA = "retrolambda"
    public static final String SCALA = "scala"
    public static final String FORCED_OKBUCK = "forcedOkbuck"
    public static final String OKBUCK_DEFS = ".okbuck/defs/DEFS"

    public static final String OKBUCK_STATE_DIR = ".okbuck/state"
    public static final String OKBUCK_STATE = "${OKBUCK_STATE_DIR}/STATE"
    public static final String OKBUCK_GEN = ".okbuck/gen"

    public static final String DEFAULT_BUCK_VERSION = "f60f4eac3f885ae839371fc0272d835253c656cc"

    // Project level globals
    public DependencyCache depCache
    public DependencyCache lintDepCache
    public TargetCache targetCache
    public String retrolambdaCmd
    public final Map<Project, Map<String, Scope>> scopes = new HashMap<>()

    void apply(Project project) {
        // Create extensions
        OkBuckExtension okbuckExt = project.extensions.create(OKBUCK, OkBuckExtension, project)
        WrapperExtension wrapper = okbuckExt.extensions.create(WRAPPER, WrapperExtension)
        ExperimentalExtension experimental = okbuckExt.extensions.create(EXPERIMENTAL, ExperimentalExtension)
        TestExtension test = okbuckExt.extensions.create(TEST, TestExtension)
        LintExtension lint = okbuckExt.extensions.create(LINT, LintExtension, project)
        RetrolambdaExtension retrolambda = okbuckExt.extensions.create(RETROLAMBDA, RetrolambdaExtension)
        ScalaExtension scala = okbuckExt.extensions.create(SCALA, ScalaExtension)

        okbuckExt.extensions.create(INTELLIJ, IntellijExtension)
        okbuckExt.extensions.create(TRANSFORM, TransformExtension)

        // Create configurations
        project.configurations.maybeCreate(TransformUtil.CONFIGURATION_TRANSFORM)
        project.configurations.maybeCreate(FORCED_OKBUCK)

        // Create tasks
        Task setupOkbuck = project.task('setupOkbuck')
        setupOkbuck.setGroup(GROUP)
        setupOkbuck.setDescription("Setup okbuck cache and dependencies")

        Task okBuck = project.tasks.create(OKBUCK, OkBuckTask, {
            okBuckExtension = okbuckExt
            scalaExtension = scala
        })
        okBuck.dependsOn(setupOkbuck)

        // Create target cache
        targetCache = new TargetCache()

        project.afterEvaluate {
            // Create wrapper task
            project.tasks.create(BUCK_WRAPPER, BuckWrapperTask, {
                repo = wrapper.repo
                watch = wrapper.watch
                sourceRoots = wrapper.sourceRoots
                ignoredDirs = wrapper.ignoredDirs
            })

            // Create extra dependency caches if needed
            Map<String, Configuration> extraConfigurations = okbuckExt.extraDepCaches.collectEntries { String cacheName ->
                [cacheName, project.configurations.maybeCreate("${cacheName}ExtraDepCache")]
            }

            setupOkbuck.doFirst {
                if (!System.getProperty("okbuck.wrapper", "false").toBoolean()) {
                    throw new IllegalArgumentException("Okbuck cannot be invoked without 'okbuck.wrapper' set to true. Use buckw instead")
                }
            }

            // Configure setup task
            setupOkbuck.doLast {
                // Cleanup gen folder
                FileUtil.deleteQuietly(project.projectDir.toPath().resolve(OKBUCK_GEN))

                okbuckExt.buckProjects.each {
                    targetCache.getTargets(it)
                }

                File cacheDir = DependencyUtils.createCacheDir(project, DEFAULT_CACHE_PATH, EXTERNAL_DEP_BUCK_FILE)
                depCache = new DependencyCache(project, cacheDir, FORCED_OKBUCK)

                // Fetch Lint deps if needed
                if (!lint.disabled && lint.version != null) {
                    LintUtil.fetchLintDeps(project, lint.version)
                }

                // Fetch transform deps if needed
                if (experimental.transform) {
                    TransformUtil.fetchTransformDeps(project)
                }

                // Fetch Retrolambda deps if needed
                boolean hasRetrolambda = okbuckExt.buckProjects.find {
                    it.plugins.hasPlugin('me.tatarka.retrolambda')
                } != null
                if (hasRetrolambda) {
                    RetrolambdaUtil.fetchRetrolambdaDeps(project, retrolambda)
                }

                // Fetch robolectric deps if needed
                if (test.robolectric) {
                    RobolectricUtil.download(project)
                }

                extraConfigurations.each { String cacheName, Configuration extraConfiguration ->
                    new DependencyCache(project,
                            DependencyUtils.createCacheDir(
                                    project, "${EXTRA_DEP_CACHE_PATH}/${cacheName}", EXTERNAL_DEP_BUCK_FILE))
                            .build(extraConfiguration)
                }
            }

            // Create clean task
            Task okBuckClean = project.tasks.create(OKBUCK_CLEAN, OkBuckCleanTask, {
                projects = okbuckExt.buckProjects
            })
            okBuck.dependsOn(okBuckClean)

            // Configure buck projects
            okbuckExt.buckProjects.findAll { it.buildFile.exists() }.each { Project buckProject ->
                buckProject.configurations.maybeCreate(BUCK_LINT)
                Task okbuckProjectTask = buckProject.tasks.maybeCreate(OKBUCK)
                okbuckProjectTask.doLast {
                    BuckFileGenerator.generate(buckProject)
                }
                okbuckProjectTask.dependsOn(setupOkbuck)
                okBuckClean.dependsOn(okbuckProjectTask)
            }
        }
    }
}
