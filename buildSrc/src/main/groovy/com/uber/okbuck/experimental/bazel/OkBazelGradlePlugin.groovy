package com.uber.okbuck.experimental.bazel

import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.dependency.DependencyCache
import com.uber.okbuck.core.model.base.TargetCache
import com.uber.okbuck.extension.OkBuckExtension
import com.uber.okbuck.extension.WrapperExtension
import com.uber.okbuck.wrapper.BuckWrapperTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration

/**
 * The {@code com.uber.okbazel} plugin. It generates <a href="https://bazel.build">Bazel</a> BUILD
 * files from an existing Android or Java Gradle project.
 */
class OkBazelGradlePlugin extends OkBuckGradlePlugin {
    // Every Bazel project must contain a top-level file called WORKSPACE. This file can be empty,
    // but to build Android projects it must contain an android_sdk_repository rule that points to
    // the location of an Android SDK. Projects with native code will also require an
    // android_ndk_repository rule, however OkBazel does not yet support native code.
    private static final String WORKSPACE = """android_sdk_repository(
    name = "androidsdk",
    path = "${System.getenv("ANDROID_HOME")}",
    build_tools_version = "25.0.0",
    api_level = 25,
)
"""

    // Instead of setting the visibility attribute on every target, we declare all targets to be
    // publicly visible by default.
    private static final String BUILD_FILE_HEADER =
            """package(default_visibility = ["//visibility:public"])"""

    private static final String WRAPPER = "wrapper"
    private static final String BAZEL_WRAPPER = "bazelWrapper"
    private static final String GROUP = "okbazel"

    // Due a known issue in Bazel (https://github.com/bazelbuild/bazel/issues/1998), the cache
    // path cannot begin with '.' as the okbuck plugin cache does.
    static final String CACHE_PATH = "okbazel/cache"

    @Override
    void apply(Project project) {
        def okBuckExt = project.extensions.create("okbuck", OkBuckExtension, project)
        def wrapper = project.extensions.create(WRAPPER, WrapperExtension)
        def externalOkbuck = project.configurations.maybeCreate(CONFIGURATION_EXTERNAL)

        Task okBazel = project.task("okbazel")
        okBazel.setGroup(GROUP)
        okBazel.setDescription("Generate Bazel BUILD files")
        okBazel.outputs.upToDateWhen { false }

        targetCache = new TargetCache()

        project.afterEvaluate {
            def bazelWrapper = project.tasks.create(BAZEL_WRAPPER, BuckWrapperTask, { t ->
                t.repo = wrapper.repo
                t.remove = ["**/BUILD"]
                t.keep = []
                t.watch = wrapper.watch
                t.sourceRoots = wrapper.sourceRoots
                t.wrapperFile = project.file('bazelw')
                t.wrapperTemplate = "wrapper/BAZELW_TEMPLATE"
            })
            bazelWrapper.setGroup(GROUP)
            bazelWrapper.setDescription("Create bazel wrapper")

            okBazel << {
                File workspaceFile = project.file("WORKSPACE")
                if (!workspaceFile.exists()) {
                    workspaceFile.write WORKSPACE
                }
                addSubProjectRepos(project as Project, okBuckExt.buckProjects as Set<Project>)
                def projectConfigurations = configurations(okBuckExt.buckProjects)
                projectConfigurations.addAll([externalOkbuck])

                depCache = new DependencyCache(
                        /* name = */ "external",
                        /* rootProject = */ project,
                        /* cacheDirPath = */ CACHE_PATH,
                        /* configurations = */ projectConfigurations,
                        /* buckFile = */ null,
                        /* cleanup = */ true,
                        /* useFullDepName = */ true,
                        /* fetchSources = */ false,
                        /* extractLintJars = */ false,
                        /* depProjects = */ okBuckExt.buckProjects)

                BuildFileGenerator.generate(okBuckExt).each { subProject, buildFile ->
                    PrintStream printer = new PrintStream(subProject.file("BUILD")) {
                        @Override
                        void println(String s) {
                            // BUILD files are typically space-delimited. Since
                            // com.uber.okbuck.rule.base.BuckRule uses tabs and all the Bazel rules
                            // inherit from that rule, all of the Bazel rules produce tabs. Here we
                            // convert them to spaces.
                            super.println(s.replaceAll("\t", "    "))
                        }
                    }
                    printer.println(BUILD_FILE_HEADER)
                    buildFile.print(printer)
                    printer.close()
                }

                File cacheBuildFile = new File(depCache.cacheDir.parent, "BUILD")
                if (cacheBuildFile.exists()) {
                    cacheBuildFile.delete()
                }
                new DependencyCacheBuildFileWriter(okBuckExt.buckProjects, depCache)
                        .write(cacheBuildFile)
            }
        }
    }
}
