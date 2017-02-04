package com.uber.okbuck.core.task;

import com.google.common.collect.ImmutableMap;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.StreamSupport;

/**
 * A task to cleanup generated configuration files
 */
@SuppressWarnings({"WeakerAccess", "CanBeFinal", "unused", "ResultOfMethodCallIgnored"})
public class OkBuckCleanTask extends DefaultTask {

    @Input
    public String dir;

    @Input
    public Set<String> includes;

    @Input
    public Set<String> excludes;

    @TaskAction
    void clean() {
        Iterator<File> iterator = getProject().fileTree(
                ImmutableMap.of("dir", dir, "includes", includes, "excludes", excludes))
                .iterator();
        Iterable<File> iterable = () -> iterator;
        StreamSupport.stream(iterable.spliterator(), true)
                .map(File::toPath)
                .forEach(OkBuckCleanTask::deleteQuietly);
    }

    private static void deleteQuietly(Path p) {
        try {
            Files.delete(p);
        } catch (IOException ignored) {}
    }
}
