package com.uber.okbuck.composer

import com.uber.okbuck.core.model.GradleSourceGen
import com.uber.okbuck.core.model.JavaTarget
import com.uber.okbuck.rule.GradleSourceGenRule

final class GradleSourceGenRuleComposer extends AndroidBuckRuleComposer {

    private GradleSourceGenRuleComposer() {
        // no instance
    }

    static List<GradleSourceGenRule> compose(JavaTarget target, String gradlePath) {
        target.gradleSourcegen.collect { GradleSourceGen sourceTask ->
            String taskName = sourceTask.task.name.replaceAll(':', '_')
            new GradleSourceGenRule(
                    "gradle_sourcegen${taskName}" as String,
                    target.rootProject.projectDir.absolutePath,
                    gradlePath,
                    sourceTask.task.path,
                    sourceTask.inputs,
                    sourceTask.outputDir.path
            )
        }
    }
}
