package com.github.okbuilds.okbuck.composer

import com.github.okbuilds.core.model.GradleSourcegen
import com.github.okbuilds.core.model.JavaTarget
import com.github.okbuilds.okbuck.rule.GradleSourceGenRule

final class GradleSourceGenRuleComposer extends AndroidBuckRuleComposer {

    private GradleSourceGenRuleComposer() {
        // no instance
    }

    static List<GradleSourceGenRule> compose(JavaTarget target, String gradlePath) {
        target.gradleSourcegen.collect { GradleSourcegen sourceTask ->
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
