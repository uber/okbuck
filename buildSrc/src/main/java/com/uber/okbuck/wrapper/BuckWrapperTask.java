package com.uber.okbuck.wrapper;

import com.google.common.collect.ImmutableMap;
import com.uber.okbuck.core.util.ReplaceUtil;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings({"WeakerAccess", "CanBeFinal", "unused", "ResultOfMethodCallIgnored"})
public class BuckWrapperTask extends DefaultTask {

    @Input
    public String repo;

    @Input
    public Set<String> remove;

    @Input
    public Set<String> keep;

    @Input
    public Set<String> watch;

    @Input
    public Set<String> sourceRoots;

    private final File wrapper = getProject().file("buckw");

    @TaskAction
    void installWrapper() {
        Map<String, String> templates = ImmutableMap.<String, String>builder()
                .put("template-creation-time", new Date().toString())
                .put("template-custom-buck-repo", repo)
                .put("template-remove", toWatchmanMatchers(remove))
                .put("template-keep", toWatchmanMatchers(keep))
                .put("template-watch", toWatchmanMatchers(watch))
                .put("template-source-roots", toWatchmanMatchers(sourceRoots))
                .build();

        ReplaceUtil.copyResourceToProject("wrapper/BUCKW_TEMPLATE", wrapper, templates);
        wrapper.setExecutable(true);
    }

    private static String toWatchmanMatchers(Set<String> wildcardPatterns) {
        return wildcardPatterns
                .parallelStream()
                .map(pattern -> "            [\"imatch\", \"" + pattern + "\", \"wholename\"]")
                .collect(Collectors.joining(" "));
    }
}
