package com.uber.okbuck.core.util;

import com.uber.okbuck.OkBuckGradlePlugin;
import com.uber.okbuck.core.dependency.DependencyCache;

import org.gradle.api.Project;

import java.util.Collections;

public final class TransformUtil {

    private static final String CONFIGURATION_TRANSFORM = "transform";
    public static final String TRANSFORM_CACHE = OkBuckGradlePlugin.DEFAULT_CACHE_PATH + "/transform";
    private static final String TRANSFORM_BUCK_FILE = "transform/BUCK_FILE";

    public static final String TRANSFORM_RULE = "//" + TRANSFORM_CACHE + ":okbuck_transform";

    private TransformUtil() { }

    public static void fetchTransformDeps(Project project) {
        new DependencyCache("transform",
                project.getRootProject(),
                TRANSFORM_CACHE,
                Collections.singleton(project.getConfigurations().getByName(CONFIGURATION_TRANSFORM)),
                TRANSFORM_BUCK_FILE);
    }
}
