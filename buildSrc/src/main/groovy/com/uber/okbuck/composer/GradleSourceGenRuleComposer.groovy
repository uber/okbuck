package com.uber.okbuck.composer

import com.uber.okbuck.core.model.GradleSourceGen
import com.uber.okbuck.core.model.JavaTarget
import com.uber.okbuck.core.util.FileUtil
import com.uber.okbuck.extension.GradleGenExtension
import com.uber.okbuck.rule.GenRule

final class GradleSourceGenRuleComposer extends AndroidBuckRuleComposer {

    private GradleSourceGenRuleComposer() {
        // no instance
    }

    static List<GenRule> compose(JavaTarget target, GradleGenExtension gradleGen) {
        return target.gradleSourcegen.collect { GradleSourceGen sourceTask ->
            String taskName = sourceTask.task.name.replaceAll(':', '_')
            List<String> gradleGenCmds = []

            gradleGenCmds.add("\$OKBUCK_PROJECT_ROOT/${gradleGen.gradle.name}")
            gradleGenCmds.add("-p \$OKBUCK_PROJECT_ROOT")
            gradleGenCmds.add(sourceTask.task.path)
            gradleGenCmds.add(gradleGen.options.join(' '))

            String gradleOutputPath = FileUtil.getRelativePath(target.project.rootDir, sourceTask.outputDir)
            gradleGenCmds.add("&& ${gradleGen.symlinkOutputs ? 'ln -sf' : 'cp -a'} " +
                    "\$OKBUCK_PROJECT_ROOT/${gradleOutputPath} \$OUT")

            new GenRule(
                    "gradle_sourcegen_${taskName}" as String,
                    sourceTask.inputs as List,
                    gradleGenCmds,
                    true)
        }
    }
}
