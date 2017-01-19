package com.uber.okbuck.composer.android

import com.uber.okbuck.core.model.android.AndroidAppTarget
import com.uber.okbuck.core.util.FileUtil
import com.uber.okbuck.core.util.TransformUtil
import com.uber.okbuck.rule.base.GenRule
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

final class TrasformDependencyWriterRuleComposer extends AndroidBuckRuleComposer {

    static final String OPT_TRANSFORM_CLASS = "transform"
    static final String OPT_CONFIG_FILE = "configFile"
    static final String RUNNER_MAIN_CLASS = "com.ubercab.transform.CliTransform"

    private TrasformDependencyWriterRuleComposer() {}

    static List<GenRule> compose(AndroidAppTarget target) {
        return target.transforms.collect { Map<String, String> options ->
            compose(target, options)
        }
    }

    static GenRule compose(AndroidAppTarget target, Map<String, String> options) {
        String transformClass = options.get(OPT_TRANSFORM_CLASS)
        String configFile = options.get(OPT_CONFIG_FILE)

        List<String> input = []
        if (configFile != null) {
            input.add(getTransformConfigRuleForFile(target.project, target.rootProject.file(configFile)))
        }

        String output = "\$OUT"
        List<String> cmds = [
                "echo \"#!/bin/bash\" > ${output};",
                "echo \"set -e\" >> ${output};",

                "echo \"export IN_JARS_DIR=\\\$1\" >> ${output};",
                "echo \"export OUT_JARS_DIR=\\\$2\" >> ${output};",
                "echo \"export ANDROID_BOOTCLASSPATH=\\\$3\" >> ${output};",

                configFile != null ? "echo \"export CONFIG_FILE=\$SRCS\" >> ${output};" : "",
                transformClass != null ? "echo \"export TRANSFORM_CLASS=${transformClass}\" >> ${output};" : "",

                "echo \"java -cp \$(location ${TransformUtil.TRANSFORM_RULE}) ${RUNNER_MAIN_CLASS}\" >> ${output};",
                "chmod +x ${output}"]

        return new GenRule(getTransformRuleName(target, options), input, cmds, true)
    }

    static getTransformRuleName(AndroidAppTarget target, Map<String, String> options) {
        return transform(options.get(OPT_TRANSFORM_CLASS), target)
    }

    static String getTransformConfigRuleForFile(Project project, File config) {
        String path = getTransformFilePathForFile(project, config)
        File configFile = new File("${TransformUtil.TRANSFORM_CACHE}/${path}")
        FileUtils.copyFile(config, configFile)
        return "//${TransformUtil.TRANSFORM_CACHE}:${path}"
    }

    private static String getTransformFilePathForFile(Project project, File config) {
        return FileUtil.getRelativePath(project.rootDir, config).replaceAll('/', '_')
    }
}
