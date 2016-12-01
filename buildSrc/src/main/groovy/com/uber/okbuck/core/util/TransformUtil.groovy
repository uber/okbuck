package com.uber.okbuck.core.util

import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.dependency.DependencyCache
import com.uber.okbuck.core.model.Scope
import org.gradle.api.Project

class TransformUtil {

    static final String CONFIGURATION_TRANSFORM = "transform"

    static final String TRANSFORM_CACHE = "${OkBuckGradlePlugin.DEFAULT_CACHE_PATH}/transform"
    static final String TRANSFORM_RULE = "//${TRANSFORM_CACHE}:okbuck_transform"
    static final String TRANSFORM_BUCK_FILE = "transform/BUCK_FILE"

    static void fetchTransformDeps(Project project) {
        File res = null
        Set<File> sourceDirs = []
        List<String> jvmArguments = []
        Scope transformScope = new Scope(
                project, [CONFIGURATION_TRANSFORM], sourceDirs, res, jvmArguments, getTransformDepsCache(project))
        transformScope.externalDeps
    }

    static DependencyCache getTransformDepsCache(Project project) {
        return new DependencyCache(project.rootProject, TRANSFORM_CACHE, false, TRANSFORM_BUCK_FILE) {

            @Override
            boolean isValid(File dep) {
                return dep.name.endsWith(".jar")
            }
        }
    }
}
