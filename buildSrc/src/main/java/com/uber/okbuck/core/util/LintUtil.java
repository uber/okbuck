package com.uber.okbuck.core.util;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.model.base.Target;
import com.uber.okbuck.extension.OkBuckExtension;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;

public final class LintUtil {

    private static final String LINT_GROUP = "com.android.tools.lint";
    private static final String LINT_MODULE = "lint";
    private static final String LINT_DEPS_CONFIG = OkBuckGradlePlugin.BUCK_LINT + "_deps";
    private static final String LINT_DEPS_CACHE = OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/lint";
    private static final String LINT_VERSION_FILE = LINT_DEPS_CACHE + "/.lintVersion";
    private static final String LINT_DEPS_BUCK_FILE = "lint/BUCK_FILE";

    private LintUtil() {}

    @Nullable
    public static String getDefaultLintVersion(Project project) {
        return project.getBuildscript()
                .getConfigurations()
                .getByName("classpath")
                .getResolvedConfiguration()
                .getResolvedArtifacts()
                .parallelStream()
                .filter(resolvedArtifact -> {
                    ModuleVersionIdentifier identifier = resolvedArtifact.getModuleVersion().getId();
                    return (LINT_GROUP.equals(identifier.getGroup()) && LINT_MODULE.equals(identifier.getName()));
                })
                .findFirst()
                .map(r -> r.getModuleVersion().getId().getVersion())
                .orElse(null);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void fetchLintDeps(Project project, String version) {
        // Invalidate lint deps when versions change
        File lintVersionFile = project.file(LINT_VERSION_FILE);
        try {
            if (!lintVersionFile.exists()
                    || !FileUtils.readFileToString(lintVersionFile).equals(version)) {
                FileUtils.deleteDirectory(lintVersionFile.getParentFile());
                lintVersionFile.getParentFile().mkdirs();
                Files.write(lintVersionFile.toPath(), Collections.singleton(version));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        project.getConfigurations().maybeCreate(LINT_DEPS_CONFIG);
        project.getDependencies().add(LINT_DEPS_CONFIG, LINT_GROUP + ":" + LINT_MODULE + ":" + version);

        getLintDepsCache(project);
    }

    private static String getLintwConfigName(Target target, File config) {
        return FileUtil.getRelativePath(target.getRootProject().getRootDir(), config).replaceAll("/", "_");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static synchronized String getLintwConfigRule(Target target, File config) {
        File configFile = target.getRootProject().file(LINT_DEPS_CACHE + "/" + getLintwConfigName(target, config));
        try {
            FileUtils.copyFile(config, configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return BuckRuleComposer.rootPrefix(target) + LINT_DEPS_CACHE + ":" + getLintwConfigName(target, config);
    }

    public static DependencyCache getLintDepsCache(Project project) {
        OkBuckGradlePlugin okBuckGradlePlugin = ProjectUtil.getPlugin(project);
        if (okBuckGradlePlugin.lintDepCache == null) {
            OkBuckExtension okBuckExtension = project.getExtensions().getByType(OkBuckExtension.class);
            okBuckGradlePlugin.lintDepCache = new DependencyCache("lint",
                    project.getRootProject(),
                    LINT_DEPS_CACHE,
                    Collections.<Configuration>singleton(project.getRootProject().getConfigurations().getByName
                            (LINT_DEPS_CONFIG)),
                    LINT_DEPS_BUCK_FILE,
                    true,
                    false,
                    false,
                    false,
                    okBuckExtension.buckProjects);
        }
        return okBuckGradlePlugin.lintDepCache;
    }

    public static String getLintDepsRule(Target target) {
        return BuckRuleComposer.rootPrefix(target) + LINT_DEPS_CACHE + ":okbuck_lint";
    }
}
