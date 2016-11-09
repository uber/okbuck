package com.uber.okbuck.composer

import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.dependency.DependencyCache
import com.uber.okbuck.core.model.JavaLibTarget
import com.uber.okbuck.core.model.Scope
import com.uber.okbuck.core.util.FileUtil
import com.uber.okbuck.rule.GenRule
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

import java.nio.file.Files

final class TrasformDependencyWriterRuleComposer extends JavaBuckRuleComposer {

    static final String OPT_TRANSFORM_PROVIDER_CLASS = "provider"
    static final String OPT_TRANSFORM_CLASS = "transform"
    static final String OPT_CONFIG_FILE = "configFile"

    static final String TRANSFORM_CACHE = "${OkBuckGradlePlugin.DEFAULT_CACHE_PATH}/transform"
    static final String TRANSFORM_BUCK_FILE = "transform/BUCK_FILE"
    static final String TRANSFORM_RULE = "//${TRANSFORM_CACHE}:okbuck_transform"
    static final String CONFIGURATION_TRANSFORM = "transform"

    private TrasformDependencyWriterRuleComposer() {}

    static List<GenRule> compose(JavaLibTarget target) {
        List<GenRule> rules = []
        if (target.transforms != null) {
            target.transforms.each { Map<String, String> options ->
                rules.add(TrasformDependencyWriterRuleComposer.compose(target, options))
            }
        }
        return rules;
    }

    static GenRule compose(JavaLibTarget target, Map<String, String> options) {

        String runnerMainClass = target.transformRunnerClass
        String providerClass = options.get(OPT_TRANSFORM_PROVIDER_CLASS)
        String transformClass = options.get(OPT_TRANSFORM_CLASS)
        String configFile = options.get(OPT_CONFIG_FILE);

        List<String> input = []
        if (configFile != null) {
            input.add(getTransformConfigRuleForFile(target.project, new File(configFile)))
        }


        String output = "\$OUT"
        List<String> cmds = [
                "echo \"#!/bin/bash\" > ${output};",
                "echo \"set -e\" >> ${output};",

                "echo \"export IN_JARS_DIR=\\\$1\" >> ${output};",
                "echo \"export OUT_JARS_DIR=\\\$2\" >> ${output};",
                "echo \"export ANDROID_BOOTCLASSPATH=\\\$3\" >> ${output};",

                configFile != null ? "echo \"export CONFIG_FILE=\$SRCS\" >> ${output};" : "",
                providerClass != null ? "echo \"export TRANSFORM_PROVIDER_CLASS=${providerClass}\" >> ${output};" : "",
                transformClass != null ? "echo \"export TRANSFORM_CLASS=${transformClass}\" >> ${output};" : "",

                "echo \"java -cp \$(location ${TRANSFORM_RULE}) ${runnerMainClass}\" >> ${output};",
                "chmod +x ${output}"]

        //When the java command runs IN_JARS_DIR and OUT_JARS_DIR are already set by buck.
        return new GenRule(getTransformRuleName(target, options), input, cmds).setExecutable(true)
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

    private static getTransformRuleName(JavaLibTarget target, Map<String, String> options) {
        String providerClass = options.get(OPT_TRANSFORM_PROVIDER_CLASS)
        String transformClass = options.get(OPT_TRANSFORM_CLASS)
        String name = providerClass != null ? providerClass : transformClass
        return transformScript(name, target);
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

    private static String getTransformFilePathForFile(Project project, File config) {
        return FileUtil.getRelativePath(project.rootDir, config).replaceAll('/', '_')
    }

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
