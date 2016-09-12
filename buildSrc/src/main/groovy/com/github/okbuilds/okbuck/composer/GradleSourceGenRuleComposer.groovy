package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.GradleSourcegen
import com.github.okbuilds.core.model.JavaTarget
import com.github.okbuilds.okbuck.rule.GradleSourcegenRule

final class GradleSourcegenRuleComposer extends AndroidBuckRuleComposer {

    private GradleSourcegenRuleComposer() {
        // no instance
    }

    static List<GradleSourcegenRule> compose(JavaTarget target, String gradlePath) {
        target.gradleSourcegen.collect { GradleSourcegen sourceTask ->
            String taskName = sourceTask.task.name.replaceAll(':', '_')
            new GradleSourcegenRule(
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
