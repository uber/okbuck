/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Piasy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.piasy.okbuck.helper

import org.gradle.api.Project

/**
 * String util class.
 * */
public final class FileUtil {

    private FileUtil() {
        // no instance
    }

    /**
     * get path diff between (sub) project and root project
     *
     * @return path diff, with prefix {@code File.separator}
     * */
    public static String getProjectPathDiff(Project rootProject, Project project) {
        return getDirPathDiff(rootProject.projectDir, project.projectDir)
    }

    /**
     * get path diff between (sub) dir and root dir
     *
     * @return path diff, with prefix {@code File.separator}
     * */
    public static String getDirPathDiff(File rootDir, File dir) {
        String rootPath = rootDir.absolutePath
        String path = dir.absolutePath
        if (path.indexOf(rootPath) == 0) {
            return path.substring(rootPath.length())
        } else {
            throw new IllegalArgumentException("sub dir ${dir.name} must " +
                    "locate inside root dir ${rootDir.name}")
        }
    }

    /**
     * simple detect way using naming convention.
     * */
    public static boolean areDepFilesDuplicated(File first, File second) {
        if (!first.name.contains("-") || !second.name.contains("-")) {
            return false
        }
        String firstDepNameSuffix = first.name.substring(first.name.lastIndexOf("."))
        if (second.name.endsWith(".jar")) {
            String secondVersion = second.name.substring(second.name.lastIndexOf("-") + 1,
                    second.name.indexOf(".jar"))
            String secondName = second.name.substring(0, second.name.lastIndexOf("-"))
            String firstVersion = first.name.substring(first.name.lastIndexOf("-") + 1,
                    first.name.indexOf(firstDepNameSuffix))
            String firstName = first.name.substring(0, first.name.lastIndexOf("-"))
            if (secondVersion.equals("debug") || secondVersion.equals("release") ||
                    firstVersion.equals("debug") || firstVersion.equals("release")) {
                return false
            }
            return secondName.equals(firstName)
        } else if (second.name.endsWith(".aar")) {
            String secondVersion = second.name.substring(second.name.lastIndexOf("-") + 1,
                    second.name.indexOf(".aar"))
            String secondName = second.name.substring(0, second.name.lastIndexOf("-"))
            String firstVersion = first.name.substring(first.name.lastIndexOf("-") + 1,
                    first.name.indexOf(firstDepNameSuffix))
            String firstName = first.name.substring(0, first.name.lastIndexOf("-"))
            if (secondVersion.equals("debug") || secondVersion.equals("release") ||
                    firstVersion.equals("debug") || firstVersion.equals("release")) {
                return false
            }
            return secondName.equals(firstName)
        }
        return false
    }
}
