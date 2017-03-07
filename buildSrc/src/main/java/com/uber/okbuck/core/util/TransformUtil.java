package com.uber.okbuck.core.util;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;

import org.gradle.api.Project;

import java.io.File;
import java.util.Collections;

public final class TransformUtil {

    public static final String TRANSFORM_CACHE = OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/transform";

    public static final String CONFIGURATION_TRANSFORM = "transform";
    private static final String TRANSFORM_FOLDER = "transform/";
    private static final String TRANSFORM_BUCK_FILE = "BUCK_FILE";
    private static final String TRANSFORM_JAR = "transform-cli-1.0.0.jar";

    public static final String TRANSFORM_RULE = "//" + TRANSFORM_CACHE + ":okbuck_transform";

    private TransformUtil() { }

    public static void fetchTransformDeps(Project project) {
        DependencyCache dependencyCache = new DependencyCache("transform",
                project.getRootProject(),
                TRANSFORM_CACHE,
                Collections.singleton(project.getConfigurations().getByName(CONFIGURATION_TRANSFORM)),
                TRANSFORM_FOLDER + TRANSFORM_BUCK_FILE);

        FileUtil.copyResourceToProject(
                TRANSFORM_FOLDER + TRANSFORM_BUCK_FILE, new File(dependencyCache.getCacheDir(), "BUCK"));
        FileUtil.copyResourceToProject(
                TRANSFORM_FOLDER + TRANSFORM_JAR, new File(dependencyCache.getCacheDir(), TRANSFORM_JAR));
    }
}
