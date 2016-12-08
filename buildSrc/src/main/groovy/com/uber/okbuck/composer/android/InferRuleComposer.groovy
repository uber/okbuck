package com.uber.okbuck.composer.android

import com.uber.okbuck.composer.java.JavaBuckRuleComposer
import com.uber.okbuck.core.model.java.JavaTarget
import com.uber.okbuck.rule.base.GenRule

class InferRuleComposer extends JavaBuckRuleComposer {

    private InferRuleComposer() {
        // no instance
    }

    static GenRule compose(JavaTarget target) {
        List<String> inferCmds = []
        List<String> inputs = []
        String sources = ""

        inferCmds.add("mkdir -p \$OUT;")

        target.main.sources.each { String sourceDir ->
            sources += sourceDir + ":"
            inputs.add(sourceDir)
        }
        if (sources.length() != 0) {
            sources = sources.substring(0, (sources.length() - 1))
        }

        inferCmds.add("infer --fail-on-issue --results-dir \$OUT --sourcepath src --generated-classes ${toLocation(":${src(target)}")} --classpath \$(classpath :${src(target)}) -- genrule")

        return new GenRule(
                "infer_${target.name}",
                inputs,
                inferCmds
        )
    }
}
