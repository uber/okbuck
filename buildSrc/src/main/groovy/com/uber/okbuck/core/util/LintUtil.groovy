package com.uber.okbuck.core.util

import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.dependency.DependencyCache
import com.uber.okbuck.core.model.JavaTarget
import com.uber.okbuck.core.model.Scope
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact

import java.nio.file.Files

class LintUtil {

    static final String LINT_GROUP = "com.android.tools.lint"
    static final String LINT_MODULE = "lint"
    static final String LINTW_LOCATION = ".okbuck/lintw"
    static final String LINTW_BUCK_FILE = "lint/LINTW_BUCK_FILE"
    static final String LINTW_TEMPLATE = "lint/LINTW_TEMPLATE"
    static final File LINTW_BUCK = new File("${LINTW_LOCATION}/BUCK")

    static final String LINT_DEPS_CONFIG = "${OkBuckGradlePlugin.BUCK_LINT}_deps"
    static final String LINT_DEPS_CACHE = "${OkBuckGradlePlugin.DEFAULT_CACHE_PATH}/lintDeps"
    static final String LINT_DEPS_RULE = "//${LINT_DEPS_CACHE}:okbuck_lint_deps"
    static final String LINT_DEPS_BUCK_FILE = "lint/LINT_DEPS_BUCK_FILE"

    static final String LINT_CACHE = "${OkBuckGradlePlugin.DEFAULT_CACHE_PATH}/lint"

    private LintUtil() {}

    static String getDefaultLintVersion(Project project) {
        ResolvedArtifact lintArtifact = project.buildscript.configurations.classpath.resolvedConfiguration
                .resolvedArtifacts.find {
            ResolvedArtifact artifact ->
                ModuleVersionIdentifier identifier = artifact.moduleVersion.id
                return (identifier.group == LINT_GROUP && identifier.name == LINT_MODULE)
        }
        if (lintArtifact) {
            return lintArtifact.moduleVersion.id.version
        } else {
            return null
        }
    }

    static void fetchLintDeps(Project project, String version) {
        if (!version) {
            throw new IllegalStateException("Invalid lint jar version: ${version}")
        }

        project.configurations.maybeCreate(LINT_DEPS_CONFIG)
        project.dependencies {
            "${LINT_DEPS_CONFIG}" "${LINT_GROUP}:${LINT_MODULE}:${version}"
        }

        File res = null
        Set<File> sourceDirs = []
        List<String> jvmArguments = []
        Scope lintDepsScope = new Scope(project, [LINT_DEPS_CONFIG], sourceDirs, res, jvmArguments,
                getLintDepsCache(project))
        lintDepsScope.getExternalDeps()
    }

    static File createLintw(JavaTarget target) {
        File lintw = new File(target.rootProject.projectDir, getLintwPath(target))
        lintw.parentFile.mkdirs()
        lintw.createNewFile()

        FileUtil.copyResourceToProject(LINTW_TEMPLATE, lintw)
        if (!LINTW_BUCK.exists()) {
            FileUtil.copyResourceToProject(LINTW_BUCK_FILE, LINTW_BUCK)
        }
        return lintw
    }

    static String getLintwScriptName(JavaTarget target) {
        return "lintw_${target.identifier.replaceAll(':', '_')}_${target.name}"
    }

    static String getLintwPath(JavaTarget target) {
        return "${LINTW_LOCATION}/${getLintwScriptName(target)}"
    }

    static String getLintwRule(JavaTarget target) {
        return "//${LINTW_LOCATION}:${getLintwScriptName(target)}"
    }

    static String getLintwConfigName(Project project, File config) {
        return FileUtil.getRelativePath(project.rootDir, config).replaceAll('/', '_')
    }

    static String getLintwConfigRule(Project project, File config) {
        File configFile = new File("${LINTW_LOCATION}/${getLintwConfigName(project, config)}")
        if (!configFile.exists() || !FileUtils.contentEquals(configFile, config)) {
            if (configFile.exists()) {
                configFile.delete()
            } else {
                configFile.parentFile.mkdirs()
            }
            Files.copy(config.toPath(), configFile.toPath())
        }
        return "//${LINTW_LOCATION}:${getLintwConfigName(project, config)}"
    }

    static DependencyCache getLintCache(Project project) {
        return new DependencyCache(project.rootProject, LINT_CACHE) {

            @Override
            boolean isValid(File dep) {
                return dep.name.endsWith(".jar")
            }
        }
    }

    static DependencyCache getLintDepsCache(Project project) {
        return new DependencyCache(project.rootProject, LINT_DEPS_CACHE, false, LINT_DEPS_BUCK_FILE) {

            @Override
            boolean isValid(File dep) {
                return dep.name.endsWith(".jar")
            }
        }
    }
}
