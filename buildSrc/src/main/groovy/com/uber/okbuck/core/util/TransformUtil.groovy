package com.uber.okbuck.core.util

import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.dependency.DependencyCache
import org.gradle.api.Project

class TransformUtil {

    static final String CONFIGURATION_TRANSFORM = "transform"

    static final String TRANSFORM_CACHE = "${OkBuckGradlePlugin.DEFAULT_CACHE_PATH}/transform"
    static final String TRANSFORM_RULE = "//${TRANSFORM_CACHE}:okbuck_transform"
    static final String TRANSFORM_BUCK_FILE = "transform/BUCK_FILE"

    static void fetchTransformDeps(Project project) {
        new DependencyCache("transform",
                project.rootProject,
                TRANSFORM_CACHE,
                [project.configurations.getByName(CONFIGURATION_TRANSFORM)] as Set,
                TRANSFORM_BUCK_FILE)
    }
}
