package com.uber.okbuck.core.util;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.DependencyUtils;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;

public final class LintUtil {

    private static final String LINT_GROUP = "com.android.tools.lint";
    private static final String LINT_MODULE = "lint";
    private static final String LINT_DEPS_CONFIG = OkBuckGradlePlugin.BUCK_LINT + "_deps";
    private static final String LINT_DEPS_CACHE = OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/lint";
    private static final String LINT_VERSION_FILE = LINT_DEPS_CACHE + "/.lintVersion";
    private static final String LINT_DEPS_BUCK_FILE = "lint/BUCK_FILE";

    public static final String LINT_DEPS_RULE = "//" + LINT_DEPS_CACHE + ":okbuck_lint";

    private LintUtil() {}

    @Nullable
    public static String getDefaultLintVersion(Project project) {
        return ProjectUtil.findVersionInClasspath(project, LINT_GROUP, LINT_MODULE);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void fetchLintDeps(Project project, String version) {
        // Invalidate lint deps when versions change
        File lintVersionFile = project.file(LINT_VERSION_FILE);
        try {
            if (!lintVersionFile.exists()
                    || !version.equals(new String(Files.readAllBytes(lintVersionFile.toPath())))) {
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

    private static String getLintwConfigName(Project project, File config) {
        return FileUtil.getRelativePath(project.getRootDir(), config).replaceAll("/", "_");
    }


    private static HashMap<String, String> lintConfigTarget = new HashMap<>();

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static synchronized String getLintwConfigRule(Project project, File config) {
        String configName =  getLintwConfigName(project, config);

        if (!lintConfigTarget.containsKey(configName)) {
            File configFile = project.getRootProject().file(LINT_DEPS_CACHE + "/" + configName);
            try {
                FileUtils.copyFile(config, configFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            lintConfigTarget.put(configName, "//" + LINT_DEPS_CACHE + ":" + configName);
        }
        return lintConfigTarget.get(configName);
    }

    public static DependencyCache getLintDepsCache(Project project) {
        OkBuckGradlePlugin okBuckGradlePlugin = ProjectUtil.getPlugin(project);
        if (okBuckGradlePlugin.lintDepCache == null) {
            okBuckGradlePlugin.lintDepCache = new DependencyCache(project,
                    DependencyUtils.createCacheDir(project, LINT_DEPS_CACHE, LINT_DEPS_BUCK_FILE));

            okBuckGradlePlugin.lintDepCache
                    .build(project.getRootProject().getConfigurations().getByName(LINT_DEPS_CONFIG));
        }
        return okBuckGradlePlugin.lintDepCache;
    }
}
