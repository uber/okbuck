/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Piasy
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

package com.github.piasy.okbuck.dependency

import com.github.piasy.okbuck.helper.CheckUtil
import com.github.piasy.okbuck.helper.StringUtil
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * Dependency presentation.
 * */
public abstract class Dependency {

    protected static Logger logger = Logging.getLogger(Dependency)

    public static Dependency fromLocalFile(File rootProjectDir, File localFile) {
        DependencyType type
        if (localFile.name.endsWith(".jar")) {
            type = DependencyType.LocalJarDependency
        } else if (localFile.name.endsWith(".aar")) {
            type = DependencyType.LocalAarDependency
        } else {
            throw new IllegalArgumentException("Bad file extension ${localFile.name}")
        }
        return new LocalDependency(type, localFile, rootProjectDir)
    }

    public static Dependency fromMavenDependency(
            File rootProjectDir, File localFile, ResolvedDependency dependency
    ) {
        DependencyType type
        if (localFile.name.endsWith(".jar")) {
            type = DependencyType.MavenJarDependency
        } else if (localFile.name.endsWith(".aar")) {
            type = DependencyType.MavenAarDependency
        } else {
            throw new IllegalArgumentException("Bad file extension ${localFile.name}")
        }
        return new MavenDependency(type, localFile, rootProjectDir, dependency)
    }

    public static Dependency fromModule(
            File rootProjectDir, File localFile, Project module, String flavor, String variant
    ) {
        if (localFile.name.endsWith(".jar")) {
            return ModuleDependency.forJar(localFile, rootProjectDir, module)
        } else if (localFile.name.endsWith(".aar")) {
            if (StringUtil.isEmpty(flavor)) {
                return ModuleDependency.forAarWithoutFlavor(
                        localFile, rootProjectDir, module, variant)
            } else {
                return ModuleDependency.forAarWithFlavor(
                        localFile, rootProjectDir, module, flavor, variant)
            }
        } else {
            throw new IllegalArgumentException("Bad file extension ${localFile.name}")
        }
    }

    /**
     * dependency type enum.
     * */
    public static enum DependencyType {
        MavenJarDependency,
        MavenAarDependency,
        LocalJarDependency,
        LocalAarDependency,
        ModuleJarDependency,
        ModuleAarDependency
    }

    private final DependencyType mDependencyType

    /**
     * this is the only key to determine whether two dependencies are equal
     * */
    protected final File mLocalFile

    protected final File mRootProjectDir

    protected Dependency(DependencyType dependencyType, File localFile, File rootProjectDir) {
        CheckUtil.checkNotNull(localFile, "localFile can't be null")
        CheckUtil.checkNotNull(rootProjectDir, "rootProjectDir can't be null")
        mDependencyType = dependencyType
        mLocalFile = localFile
        mRootProjectDir = rootProjectDir
    }

    public DependencyType getType() {
        return mDependencyType
    }

    public File getDepFile() {
        return mLocalFile
    }

    public abstract boolean isDuplicate(Dependency dependency)

    @Override
    int hashCode() {
        return mLocalFile.hashCode()
    }

    @Override
    boolean equals(Object obj) {
        return obj != null && obj instanceof Dependency &&
                ((Dependency) obj).mLocalFile.equals(mLocalFile)
    }

    @Override
    String toString() {
        return "Dependency type ${mDependencyType}, locate at ${mLocalFile.absolutePath}"
    }

    public abstract String getSrcCanonicalName()

    public abstract List<String> getResCanonicalNames()

    public abstract boolean shouldCopy()

    public abstract void setDstDir(File dstDir)

    public abstract File getDstDir()

    public abstract void copyTo()

    public abstract Dependency defensiveCopy()
}