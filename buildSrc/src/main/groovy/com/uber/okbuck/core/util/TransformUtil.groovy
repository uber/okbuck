package com.uber.okbuck.core.util

import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.composer.JavaBuckRuleComposer
import com.uber.okbuck.core.dependency.DependencyCache
import com.uber.okbuck.core.model.JavaLibTarget
import com.uber.okbuck.core.model.Scope
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

import java.nio.file.Files

class TransformUtil {

    static final String OPT_TRANSFORM_PROVIDER_CLASS = "provider"
    static final String OPT_TRANSFORM_CLASS = "transform"
    static final String OPT_CONFIG_FILE = "configFile"

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

    static getTransformRuleName(JavaLibTarget target, Map<String, String> options) {
        String providerClass = options.get(OPT_TRANSFORM_PROVIDER_CLASS)
        String transformClass = options.get(OPT_TRANSFORM_CLASS)
        String name = providerClass != null ? providerClass : transformClass
        return JavaBuckRuleComposer.transform(name, target);
    }

    static Set<String> getTransformRules(JavaLibTarget target) {
        Set<String> transformRules = []
        if (target.transforms != null) {
            target.transforms.each { Map<String, String> options ->
                transformRules.add(":${getTransformRuleName(target, options)}")
            }
        }
        return transformRules;
    }

    static String getTransformConfigRuleForFile(Project project, File config) {
        // Adding the config file
        String path = getTransformFilePathForFile(project, config)
        File configFile = new File("${TRANSFORM_CACHE}/${path}")
        if (!configFile.exists() || !FileUtils.contentEquals(configFile, config)) {
            if (configFile.exists()) {
                configFile.delete()
            } else {
                configFile.parentFile.mkdirs()
            }
            Files.copy(config.toPath(), configFile.toPath())
        }
        return "//${TRANSFORM_CACHE}:${path}"
    }

    static String getTransformProviderClass(Map<String, String> options) {
        return options.get(OPT_TRANSFORM_PROVIDER_CLASS)
    }

    static String getTransformClass(Map<String, String> options) {
        return options.get(OPT_TRANSFORM_CLASS)
    }

    static String getConfigFile(Map<String, String> options) {
        return options.get(OPT_CONFIG_FILE)
    }

    private static String getTransformFilePathForFile(Project project, File config) {
        return FileUtil.getRelativePath(project.rootDir, config).replaceAll('/', '_')
    }
}
