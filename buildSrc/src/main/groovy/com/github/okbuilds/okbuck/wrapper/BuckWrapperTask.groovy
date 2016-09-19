package com.github.okbuilds.okbuck.wrapper

import com.github.okbuilds.core.util.FileUtil
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class BuckWrapperTask extends DefaultTask {

    @Input
    String repo

    @Input
    List<String> remove

    @Input
    List<String> keep

    File wrapper = project.file('buckw')

    @TaskAction
    void installWrapper() {
        FileUtil.copyResourceToProject("wrapper/BUCKW_TEMPLATE", wrapper)

        String outputText = wrapper.text
        outputText = outputText
                .replaceFirst('template-creation-time', new Date().toString())
                .replaceFirst('template-custom-buck-repo', repo)
                .replaceFirst('template-remove', toWatchmanMatchers(remove))
                .replaceFirst('template-keep', toWatchmanMatchers(keep))

        wrapper.text = outputText
        wrapper.setExecutable(true)
    }

    static String toWatchmanMatchers(List<String> wildcardPatterns) {
        return wildcardPatterns.collect { pattern ->
            "            [\"imatch\", \"${pattern}\", \"wholename\"]"
        }.join(",\n")
    }
}
