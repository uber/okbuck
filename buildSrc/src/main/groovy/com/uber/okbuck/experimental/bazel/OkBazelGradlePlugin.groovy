package com.uber.okbuck.experimental.bazel

import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.dependency.DependencyCache
import com.uber.okbuck.extension.OkBuckExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * The {@code com.uber.okbazel} plugin. It generates <a href="https://bazel.build">Bazel</a> BUILD
 * files from an existing Android or Java Gradle project.
 */
class OkBazelGradlePlugin implements Plugin<Project> {
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

    // Due a known issue in Bazel (https://github.com/bazelbuild/bazel/issues/1998), the cache
    // path cannot begin with '.' as the okbuck plugin cache does.
    static final String CACHE_PATH = "okbazel/cache"

    @Override
    void apply(Project project) {
        project.extensions.create("okbuck", OkBuckExtension, project)

        Task okBazel = project.task("okbazel")
        okBazel.setGroup("okbazel")
        okBazel.setDescription("Generate Bazel BUILD files")
        okBazel.outputs.upToDateWhen { false }

        project.afterEvaluate {
            okBazel << {
                File workspaceFile = project.file("WORKSPACE")
                if (!workspaceFile.exists()) {
                    workspaceFile.write WORKSPACE
                }
                OkBuckGradlePlugin.depCache =
                        new DependencyCache(project, CACHE_PATH, true, null, false, true)

                BuildFileGenerator.generate(project).each { subProject, buildFile ->
                    PrintStream printer = new PrintStream(subProject.file("BUILD")) {
                        @Override
                        public void println(String s) {
                            // BUILD files are typically space-delimited. Since
                            // com.uber.okbuck.rule.BuckRule uses tabs and all the Bazel rules
                            // inherit from that rule, all of the Bazel rules produce tabs. Here we
                            // convert them to spaces.
                            super.println(s.replaceAll("\t", "    "))
                        }
                    }
                    printer.println(BUILD_FILE_HEADER)
                    buildFile.print(printer)
                    printer.close()
                }

                File cacheBuildFile = new File(OkBuckGradlePlugin.depCache.cacheDir.parent, "BUILD")
                if (cacheBuildFile.exists()) {
                    cacheBuildFile.delete()
                }
                new DependencyCacheBuildFileWriter(OkBuckGradlePlugin.depCache)
                        .write(cacheBuildFile)
            }
        }
    }
}
