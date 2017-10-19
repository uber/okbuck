package com.uber.okbuck.core.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.composer.base.BuckRuleComposer;
import com.uber.okbuck.core.dependency.DependencyCache;
import com.uber.okbuck.core.dependency.DependencyUtils;
import com.uber.okbuck.core.model.android.AndroidAppTarget;
import com.uber.okbuck.core.model.base.Scope;
import com.uber.okbuck.template.config.TransformBuckFile;

import org.apache.commons.lang3.tuple.Pair;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class TransformUtil {

    private static final String TRANSFORM_CACHE = OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/transform";
    public static final String TRANSFORM_RULE = "//" + TRANSFORM_CACHE + ":okbuck_transform";

    public static final String CONFIGURATION_TRANSFORM = "transform";
    private static final String TRANSFORM_FOLDER = "transform/";
    private static final String TRANSFORM_JAR = "transform-cli-1.1.0.jar";

    private static final String OPT_TRANSFORM_CLASS = "transform";
    private static final String OPT_CONFIG_FILE = "configFile";
    private static final String PREFIX = "java -Dokbuck.inJarsDir=$IN_JARS_DIR -Dokbuck.outJarsDir=$OUT_JARS_DIR "
            + "-Dokbuck.androidBootClasspath=$ANDROID_BOOTCLASSPATH ";
    private static final String SUFFIX = "-cp $(location " + TransformUtil.TRANSFORM_RULE + ") "
            + "com.uber.okbuck.transform.CliTransform; ";

    private TransformUtil() { }

    public static void fetchTransformDeps(Project project) {
        Set<Configuration> transformConfigurations =
                ImmutableSet.of(project.getConfigurations().getByName(CONFIGURATION_TRANSFORM));

        File cacheDir = DependencyUtils.createCacheDir(project, TRANSFORM_CACHE);
        DependencyCache dependencyCache = new DependencyCache(project, cacheDir);
        dependencyCache.build(transformConfigurations);

        Scope transformScope = Scope.from(
                project,
                Collections.singleton(CONFIGURATION_TRANSFORM),
                ImmutableSet.of(),
                null,
                ImmutableList.of(),
                dependencyCache);

        Set<String> targetDeps = BuckRuleComposer.targets(transformScope.getTargetDeps())
                .stream()
                .map(s -> "'" + s + "'")
                .collect(MoreCollectors.toImmutableSet());

        new TransformBuckFile().targetDeps(targetDeps).render(cacheDir.toPath().resolve("BUCK"));
        FileUtil.copyResourceToProject(TRANSFORM_FOLDER + TRANSFORM_JAR, new File(cacheDir, TRANSFORM_JAR));
    }

    public static Pair<String, List<String>> getBashCommandAndTransformDeps(AndroidAppTarget target) {
        List<Pair<String, String>> results = target.getTransforms().stream().map(
                it -> getBashCommandAndTransformDeps(target, it)
        ).collect(Collectors.toList());
        return Pair.of(
          String.join(" ", results.stream().map(Pair::getLeft).collect(Collectors.toList())),
                results.stream().map(Pair::getRight).filter(Objects::nonNull).collect(Collectors.toList())
        );
    }

    private static Pair<String, String> getBashCommandAndTransformDeps(AndroidAppTarget target, Map<String, String> options) {
        String transformClass = options.get(OPT_TRANSFORM_CLASS);
        String configFile = options.get(OPT_CONFIG_FILE);
        StringBuilder bashCmd = new StringBuilder(PREFIX);

        String configFileRule = null;
        if (transformClass != null) {
            bashCmd.append("-Dokbuck.transformClass=");
            bashCmd.append(transformClass);
            bashCmd.append(" ");
        }
        if (configFile != null) {
            configFileRule =
                    getTransformConfigRuleForFile(target.getProject(), target.getRootProject().file(configFile));
            bashCmd.append("-Dokbuck.configFile=$(location ");
            bashCmd.append(configFileRule);
            bashCmd.append(") ");
        }
        bashCmd.append(SUFFIX);
        return Pair.of(bashCmd.toString(), configFileRule);
    }

    private static String getTransformConfigRuleForFile(Project project, File config) {
        String path = getTransformFilePathForFile(project, config);
        File configFile = new File(project.getRootDir(), TransformUtil.TRANSFORM_CACHE + File.separator + path);
        try {
            Files.copy(config.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) { }
        return "//" + TransformUtil.TRANSFORM_CACHE + ":" + path;
    }

    private static String getTransformFilePathForFile(Project project, File config) {
        return FileUtil.getRelativePath(project.getRootDir(), config).replaceAll("/", "_");
    }
}
