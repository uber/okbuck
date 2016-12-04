package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.core.util.FileUtil
import com.uber.okbuck.core.util.TransformUtil
import com.uber.okbuck.rule.base.GenRule
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

import java.nio.file.Files

final class TrasformDependencyWriterRuleComposer extends AndroidBuckRuleComposer {

    static final String OPT_TRANSFORM_PROVIDER_CLASS = "provider"
    static final String OPT_TRANSFORM_CLASS = "transform"
    static final String OPT_CONFIG_FILE = "configFile"

    private TrasformDependencyWriterRuleComposer() {}

    static List<GenRule> compose(AndroidAppTarget target) {
        return target.transforms.collect { Map<String, String> options ->
            compose(target, options)
        }
    }

    static GenRule compose(AndroidAppTarget target, Map<String, String> options) {
        String runnerMainClass = target.transformRunnerClass
        String providerClass = options.get(OPT_TRANSFORM_PROVIDER_CLASS)
        String transformClass = options.get(OPT_TRANSFORM_CLASS)
        String configFile = options.get(OPT_CONFIG_FILE)

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

                "echo \"java -cp \$(location ${TransformUtil.TRANSFORM_RULE}) ${runnerMainClass}\" >> ${output};",
                "chmod +x ${output}"]

        return new GenRule(getTransformRuleName(target, options), input, cmds, true)
    }

    static getTransformRuleName(AndroidAppTarget target, Map<String, String> options) {
        String providerClass = options.get(OPT_TRANSFORM_PROVIDER_CLASS)
        String transformClass = options.get(OPT_TRANSFORM_CLASS)
        String name = providerClass != null ? providerClass : transformClass
        return transform(name, target)
    }

    static String getTransformConfigRuleForFile(Project project, File config) {
        String path = getTransformFilePathForFile(project, config)
        File configFile = new File("${TransformUtil.TRANSFORM_CACHE}/${path}")
        if (!configFile.exists() || !FileUtils.contentEquals(configFile, config)) {
            if (configFile.exists()) {
                configFile.delete()
            } else {
                configFile.parentFile.mkdirs()
            }
            Files.copy(config.toPath(), configFile.toPath())
        }
        return "//${TransformUtil.TRANSFORM_CACHE}:${path}"
    }

    private static String getTransformFilePathForFile(Project project, File config) {
        return FileUtil.getRelativePath(project.rootDir, config).replaceAll('/', '_')
    }
}
