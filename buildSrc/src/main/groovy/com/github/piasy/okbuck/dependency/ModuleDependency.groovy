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
import com.github.piasy.okbuck.helper.FileUtil
import com.github.piasy.okbuck.helper.ProjectHelper
import com.github.piasy.okbuck.helper.StringUtil
import org.gradle.api.Project

import javax.naming.OperationNotSupportedException

/**
 * module dependency: java/android library module.
 *
 * only stands for one Flavor X Variant combination.
 * */
public final class ModuleDependency extends Dependency {

    private final Project mModule
    private final String mFlavor
    private final String mVariant

    public static ModuleDependency forJar(
            File localFile, File projectRootDir, Project module
    ) {
        return new ModuleDependency(DependencyType.ModuleJarDependency, localFile, projectRootDir,
                module, null, null)
    }

    public static ModuleDependency forAarWithoutFlavor(
            File localFile, File projectRootDir, Project module, String variant
    ) {
        CheckUtil.checkStringNotEmpty(variant, "ModuleDependency's variant can't be empty")
        return new ModuleDependency(DependencyType.ModuleAarDependency, localFile, projectRootDir,
                module, null, variant)
    }

    public static ModuleDependency forAarWithFlavor(
            File localFile, File projectRootDir, Project module, String flavor, String variant
    ) {
        CheckUtil.checkStringNotEmpty(flavor, "ModuleDependency's flavor can't be empty")
        CheckUtil.checkStringNotEmpty(variant, "ModuleDependency's variant can't be empty")
        return new ModuleDependency(DependencyType.ModuleAarDependency, localFile, projectRootDir,
                module, flavor, variant)
    }

    private ModuleDependency(
            DependencyType dependencyType, File localFile, File projectRootDir, Project module,
            String flavor, String variant
    ) {
        super(dependencyType, localFile, projectRootDir)
        CheckUtil.checkNotNull(module, "module can't be null")
        mModule = module
        mFlavor = flavor
        mVariant = variant
    }

    @Override
    boolean shouldCopy() {
        return false
    }

    @Override
    void setDstDir(File dstDir) {
        throw new OperationNotSupportedException("ModuleDependency should not set dstDir")
    }

    @Override
    File getDstDir() {
        throw new OperationNotSupportedException("ModuleDependency has no dstDir")
    }

    @Override
    void copyTo() {
        throw new OperationNotSupportedException("ModuleDependency should not copied")
    }

    @Override
    String getSrcCanonicalName() {
        switch (type) {
            case DependencyType.ModuleJarDependency:
            case DependencyType.ModuleAarDependency:
                return getSrcCanonicalNameWithFlavorVariant()
            default:
                throw new IllegalArgumentException("bad type of ${mLocalFile.name}")
        }
    }

    private String getSrcCanonicalNameWithFlavorVariant() {
        return "/${FileUtil.getDirPathDiff(mRootProjectDir, mModule.projectDir)}:src" +
                (StringUtil.isEmpty(mFlavor) ? "" : "_${mFlavor}") +
                ((StringUtil.isEmpty(mFlavor) || StringUtil.isEmpty(mVariant)) ? "" : "_${mVariant}")
    }

    @Override
    List<String> getResCanonicalNames() {
        List<String> presentResNames = ProjectHelper.getPresentResCanonicalNames(mModule, mFlavor, mVariant)
        List<String> ret = new ArrayList<>()
        for (String name : presentResNames) {
            ret.add("/${FileUtil.getDirPathDiff(mRootProjectDir, mModule.projectDir)}:${name}")
        }
        return ret
    }

    @Override
    boolean isDuplicate(Dependency dependency) {
        switch (dependency.type) {
            case DependencyType.ModuleJarDependency:
            case DependencyType.ModuleAarDependency:
                ModuleDependency that = (ModuleDependency) dependency
                return this.mModule.projectDir.equals(that.mModule.projectDir)
            default:
                return false
        }
    }

    @Override
    Dependency defensiveCopy() {
        return this
    }
}