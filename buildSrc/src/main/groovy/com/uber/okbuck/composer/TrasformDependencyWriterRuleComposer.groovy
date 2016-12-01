package com.uber.okbuck.composer

import com.uber.okbuck.core.model.JavaLibTarget
import com.uber.okbuck.core.util.TransformUtil
import com.uber.okbuck.rule.GenRule

final class TrasformDependencyWriterRuleComposer extends JavaBuckRuleComposer {

    private TrasformDependencyWriterRuleComposer() {}

    static List<GenRule> compose(JavaLibTarget target) {
        List<GenRule> rules = []
        if (target.transforms != null) {
            target.transforms.each { Map<String, String> options ->
                rules.add(compose(target, options))
            }
        }
        return rules;
    }

    static GenRule compose(JavaLibTarget target, Map<String, String> options) {

        String runnerMainClass = target.transformRunnerClass
        String providerClass = TransformUtil.getTransformProviderClass(options)
        String transformClass = TransformUtil.getTransformClass(options)
        String configFile = TransformUtil.getConfigFile(options)

        List<String> input = []
        if (configFile != null) {
            input.add(TransformUtil.getTransformConfigRuleForFile(target.project, new File(configFile)))
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

        return new GenRule(TransformUtil.getTransformRuleName(target, options), input, cmds)
                .setExecutable(true)
    }
}
