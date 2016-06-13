package com.github.okbuilds.core.util

import org.gradle.api.Project

final class FileUtil {

    private FileUtil() {
        // no instance
    }

    static String getRelativePath(File rootDir, File dir) {
        String rootPath = rootDir.absolutePath
        String path = dir.absolutePath
        if (path.indexOf(rootPath) == 0) {
            return path.substring(rootPath.length() + 1)
        } else {
            throw new IllegalArgumentException("sub dir ${dir.name} must " +
                    "locate inside root dir ${rootDir.name}")
        }
    }

    static void copyResourceToProject(String resource, File destination) {
        InputStream inputStream = FileUtil.getResourceAsStream(resource)
        OutputStream outputStream = new FileOutputStream(destination)
        outputStream.write(inputStream.bytes)
        outputStream.close()
    }

    static Set<String> getAvailable(Project project, Collection<File> files) {
        return files.findAll { File file ->
            file.exists()
        }.collect { File file ->
            getRelativePath(project.projectDir, file)
        }
    }
}
