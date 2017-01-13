package com.uber.okbuck.core.util;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Optional;

public final class LintUtil {

    private static final String LINT_GROUP = "com.android.tools.lint";
    private static final String LINT_MODULE = "lint";
    private static final String LINT_DEPS_CONFIG = OkBuckGradlePlugin.BUCK_LINT + "_deps";
    private static final String LINT_DEPS_CACHE = OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/lint";
    private static final String LINT_VERSION_FILE = LINT_DEPS_CACHE + "/.lintVersion";
    private static final String LINT_DEPS_BUCK_FILE = "lint/BUCK_FILE";

    public static final String LINT_DEPS_RULE = "//" + LINT_DEPS_CACHE + ":okbuck_lint";

    private LintUtil() {}

    public static String getDefaultLintVersion(Project project) {
        Optional<ResolvedArtifact> lintArtifact = project.getBuildscript()
                .getConfigurations()
                .getByName("classpath")
                .getResolvedConfiguration()
                .getResolvedArtifacts()
                .stream()
                .filter(LintUtil::findLint)
                .findFirst();
        return lintArtifact.map(r -> r.getModuleVersion().getId().getVersion()).orElse(null);
    }

    public static void fetchLintDeps(Project project, String version) {
        // Invalidate lint deps when versions change
        File lintVersionFile = project.file(LINT_VERSION_FILE);
        try {
            if (!lintVersionFile.exists()
                    || !FileUtils.readFileToString(lintVersionFile, Charset.defaultCharset()).equals(version)) {
                FileUtils.deleteDirectory(lintVersionFile.getParentFile());
                lintVersionFile.getParentFile().mkdirs();
                FileUtils.write(lintVersionFile, version, Charset.defaultCharset());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        project.getConfigurations().maybeCreate(LINT_DEPS_CONFIG);
        project.getDependencies().add(LINT_DEPS_CONFIG, LINT_GROUP + ":" + LINT_MODULE + ":" + version);

        getLintDepsCache(project);
    }

    private static String getLintwConfigName(Project project, File config) {
        return FileUtil.getRelativePath(project.getRootDir(), config).replaceAll("/", "_");
    }

    public static synchronized String getLintwConfigRule(Project project, File config) {
        File configFile = new File(LINT_DEPS_CACHE + "/" + getLintwConfigName(project, config));
        try {
            if (!configFile.exists() || !FileUtils.contentEquals(configFile, config)) {
                if (configFile.exists()) {
                    configFile.delete();
                } else {
                    configFile.getParentFile().mkdirs();
                }
                Files.copy(config.toPath(), configFile.toPath());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "//" + LINT_DEPS_CACHE + ":" + getLintwConfigName(project, config);
    }

    private static DependencyCache getLintDepsCache(Project project) {
        OkBuckGradlePlugin okBuckGradlePlugin = ProjectUtil.getPlugin(project);
        if (okBuckGradlePlugin.lintDepCache == null) {
            okBuckGradlePlugin.lintDepCache = new DependencyCache("lint",
                    project.getRootProject(),
                    LINT_DEPS_CACHE,
                    Collections.singleton(project.getRootProject().getConfigurations().getByName(LINT_DEPS_CONFIG)),
                    LINT_DEPS_BUCK_FILE);
        }
        return okBuckGradlePlugin.lintDepCache;
    }

    private static boolean findLint(ResolvedArtifact artifact) {
        ModuleVersionIdentifier identifier = artifact.getModuleVersion().getId();
        return (LINT_GROUP.equals(identifier.getGroup()) && LINT_MODULE.equals(identifier.getName()));
    }
}
