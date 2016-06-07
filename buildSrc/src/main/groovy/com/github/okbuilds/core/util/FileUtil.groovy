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

package com.github.okbuilds.core.util

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
}
