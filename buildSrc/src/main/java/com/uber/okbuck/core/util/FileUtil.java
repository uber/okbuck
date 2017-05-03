package com.uber.okbuck.core.util;

import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class FileUtil {

    private FileUtil() {}

    public static String getRelativePath(File root, File f) {
        Path fPath = f.toPath().toAbsolutePath();
        Path rootPath = root.toPath().toAbsolutePath();
        if (fPath.startsWith(rootPath)) {
            return fPath.toString().substring(rootPath.toString().length() + 1);
        } else {
            throw new IllegalStateException(fPath + " must be located inside " + rootPath);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void copyResourceToProject(String resource, File destination) {
        destination.getParentFile().mkdirs();
        try (InputStream inputStream = FileUtil.class.getResourceAsStream(resource);
                OutputStream outputStream = new FileOutputStream(destination)) {
            IOUtils.copy(inputStream, outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void copyResourceToProject(String resource, File destination, Map<String, String> templates) {
        ReplaceUtil.copyResourceToProject(resource, destination, templates);
    }

    public static Set<String> getIfAvailable(final Project project, Collection<File> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptySet();
        }
        return files.stream()
                .filter(f -> Objects.nonNull(f) && f.exists())
                .map(f -> getRelativePath(project.getProjectDir(), f))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Nullable
    public static String getIfAvailable(Project project, File file) {
        Set<String> available = getIfAvailable(project, Collections.singleton(file));
        return available.isEmpty() ? null : available.iterator().next();
    }

    public static void deleteQuietly(Path p) {
        try {
            Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {}
    }
}
