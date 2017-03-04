package com.uber.okbuck.core.task;

import com.google.common.collect.Sets;
import com.uber.okbuck.OkBuckGradlePlugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A task to cleanup stale BUCK files
 */
@SuppressWarnings({"WeakerAccess", "CanBeFinal", "unused", "ResultOfMethodCallIgnored", "NewApi"})
public class OkBuckCleanTask extends DefaultTask {

    @Input
    public Set<Project> projects = new HashSet<>();

    @TaskAction
    void clean() throws IOException {
        Project rootProject = getProject();
        Path rootProjectPath = rootProject.getProjectDir().toPath();

        File okbuckState = rootProject.file(OkBuckGradlePlugin.OKBUCK_STATE);

        // Get last project paths
        Set<String> lastProjectPaths;
        if (okbuckState.exists()) {
            lastProjectPaths = Files.lines(okbuckState.toPath())
                    .map(String::trim)
                    .filter(s -> s != null && s.length() > 0)
                    .collect(Collectors.toSet());
        } else {
            lastProjectPaths = new HashSet<>();
            okbuckState.getParentFile().mkdirs();
            okbuckState.createNewFile();
        }

        // Get current project relative paths
        Set<String> currentProjectPaths =
                projects.stream()
                        .map(project -> rootProjectPath
                                .relativize(project.getProjectDir().toPath()).toString()
                        )
                        .collect(Collectors.toSet());

        // Delete stale project's BUCK file
        Sets.difference(lastProjectPaths, currentProjectPaths)
                .stream()
                .map(p -> rootProjectPath.resolve(p).resolve("BUCK"))
                .collect(Collectors.toSet())
                .forEach(OkBuckCleanTask::deleteQuietly);

        // Save generated project's BUCK file path
        Files.write(
                okbuckState.toPath(),
                currentProjectPaths
                        .stream()
                        .sorted()
                        .collect(Collectors.toList())
        );
    }

    private static void deleteQuietly(Path p) {
        try {
            Files.delete(p);
        } catch (IOException ignored) {}
    }
}
