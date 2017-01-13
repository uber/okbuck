package com.uber.okbuck.core.util

import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.dependency.DependencyCache
import groovy.transform.Synchronized
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact

import java.nio.file.Files

class LintUtil {

    static final String LINT_GROUP = "com.android.tools.lint"
    static final String LINT_MODULE = "lint"

    static final String LINT_DEPS_CONFIG = "${OkBuckGradlePlugin.BUCK_LINT}_deps"
    static final String LINT_DEPS_CACHE = "${OkBuckGradlePlugin.DEFAULT_CACHE_PATH}/lint"
    static final String LINT_VERSION_FILE = "${LINT_DEPS_CACHE}/.lintVersion"
    static final String LINT_DEPS_RULE = "//${LINT_DEPS_CACHE}:okbuck_lint"
    static final String LINT_DEPS_BUCK_FILE = "lint/BUCK_FILE"

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

        // Invalidate lint deps when versions change
        File lintVersionFile = project.file(LINT_VERSION_FILE)
        if (!lintVersionFile.exists() || lintVersionFile.text != version) {
            FileUtils.deleteDirectory(lintVersionFile.parentFile)
            lintVersionFile.parentFile.mkdirs()
            lintVersionFile.text = version
        }

        project.configurations.maybeCreate(LINT_DEPS_CONFIG)
        project.dependencies {
            "${LINT_DEPS_CONFIG}" "${LINT_GROUP}:${LINT_MODULE}:${version}"
        }

        getLintDepsCache(project)
    }

    static String getLintwConfigName(Project project, File config) {
        return FileUtil.getRelativePath(project.rootDir, config).replaceAll('/', '_')
    }

    @Synchronized
    static String getLintwConfigRule(Project project, File config) {
        File configFile = new File("${LINT_DEPS_CACHE}/${getLintwConfigName(project, config)}")
        if (!configFile.exists() || !FileUtils.contentEquals(configFile, config)) {
            if (configFile.exists()) {
                configFile.delete()
            } else {
                configFile.parentFile.mkdirs()
            }
            Files.copy(config.toPath(), configFile.toPath())
        }
        return "//${LINT_DEPS_CACHE}:${getLintwConfigName(project, config)}"
    }

    static DependencyCache getLintDepsCache(Project project) {
        OkBuckGradlePlugin okBuckGradlePlugin = project.rootProject.plugins.getPlugin(OkBuckGradlePlugin)
        if (!okBuckGradlePlugin.lintDepCache) {
            okBuckGradlePlugin.lintDepCache = new DependencyCache("lint",
                    project.rootProject,
                    LINT_DEPS_CACHE,
                    [project.rootProject.configurations.getByName(LINT_DEPS_CONFIG)] as Set,
                    LINT_DEPS_BUCK_FILE)
        }
        return okBuckGradlePlugin.lintDepCache
    }
}
