package com.uber.okbuck.core.util

import groovy.transform.Memoized
import org.gradle.api.Project

final class FileUtil {

    private FileUtil() {
        // no instance
    }

    @Memoized
    static String getRelativePath(File root, File f) {
        String rootPath = root.absolutePath
        String path = f.absolutePath
        if (path.indexOf(rootPath) == 0) {
            return path.substring(rootPath.length() + 1)
        } else {
            throw new IllegalArgumentException("${f.name} must be located inside ${root.name}")
        }
    }

    static void copyResourceToProject(String resource, File destination) {
        InputStream inputStream = FileUtil.getResourceAsStream(resource)
        OutputStream outputStream = new FileOutputStream(destination)
        outputStream.write(inputStream.bytes)
        outputStream.close()
    }

    static Set<String> getAvailable(Project project, Collection<File> files) {
        if (!files) {
            return []
        }

        files.collect { File file ->
            getAvailableFile(project, file)
        }.findAll { String filePath ->
            filePath
        }
    }

    static String getAvailableFile(Project project, File file) {
        if (!file || !file.exists()) {
            return null
        }

        return getRelativePath(project.projectDir, file)
    }
}
