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

package com.github.piasy.okbuck.helper

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.internal.dsl.ProductFlavor
import com.github.piasy.okbuck.OkBuckExtension
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin
/**
 * helper class for sub projects.
 * */
public final class ProjectHelper {
    private static Logger logger = Logging.getLogger(ProjectHelper)
    /**
     * sub project type enum.
     * */
    public static enum ProjectType {

        Unknown,
        AndroidAppProject,
        AndroidLibProject,
        JavaLibProject
    }

    private ProjectHelper() {
        // no instance
    }

    /**
     * get sub project type
     * */
    public static ProjectType getSubProjectType(Project project) {
        if (project.plugins.hasPlugin(AppPlugin)) {
            return ProjectType.AndroidAppProject
        } else if (project.plugins.hasPlugin(LibraryPlugin)) {
            return ProjectType.AndroidLibProject
        } else if (project.plugins.hasPlugin(JavaPlugin)) {
            return ProjectType.JavaLibProject
        } else {
            return ProjectType.Unknown
        }
    }

    /**
     * check whether the project has product flavors and exported non-default configurations.
     * */
    public static boolean exportFlavor(Project project) {
        switch (getSubProjectType(project)) {
            case ProjectType.AndroidAppProject:
                return hasFlavors(project)
            case ProjectType.AndroidLibProject:
                return publishNonDefault(project) && hasFlavors(project)
            default:
                return false
        }
    }

    private static boolean publishNonDefault(Project project) {
        return project.android.publishNonDefault
    }

    private static boolean hasFlavors(Project project) {
        return !project.android.productFlavors.empty
    }

    /**
     * get product flavors of sub project.
     * */
    public static Map<String, ProductFlavor> getProductFlavors(Project project) {
        return project.android.productFlavors.collectEntries {
            [it.name, it]
        }
    }

    /**
     * if the dependency is an module dependency, return the module dependency project, null otherwise.
     * */
    public static Project getModuleDependencyProject(Project rootProject, File dependency) {
        OkBuckExtension okbuck = rootProject.okbuck
        return okbuck.buckProjects.find { Project project ->
            dependency.absolutePath.startsWith(project.buildDir.absolutePath)
        }
    }

    public static String getVersionName(Project project, String flavor) {
        ProjectType type = getSubProjectType(project)
        String versionName = ""
        if (type == ProjectType.AndroidAppProject || type == ProjectType.AndroidLibProject) {
            if (hasFlavors(project)) {
                ProductFlavor productFlavor = getProductFlavors(project).get(flavor)
                if (productFlavor != null) {
                    versionName = productFlavor.versionName
                }
            }
            if (StringUtil.isEmpty(versionName)) {
                versionName = project.android.defaultConfig.versionName
            }
        }
        if (StringUtil.isEmpty(versionName)) {
            throw new IllegalStateException("You must specify versionName for ${project.name}")
        }
        return versionName
    }

    public static int getVersionCode(Project project, String flavor) {
        ProjectType type = getSubProjectType(project)
        int versionCode = -1
        if (type == ProjectType.AndroidAppProject || type == ProjectType.AndroidLibProject) {
            if (hasFlavors(project)) {
                ProductFlavor productFlavor = getProductFlavors(project).get(flavor)
                if (productFlavor != null) {
                    versionCode = productFlavor.versionCode
                }
            }
            if (versionCode < 0) {
                versionCode = project.android.defaultConfig.versionCode
            }
        }
        if (versionCode < 0) {
            throw new IllegalStateException("You must specify versionCode for ${project.name}")
        }
        return versionCode
    }

    public static int getMinSdkVersion(Project project, String flavor) {
        ProjectType type = getSubProjectType(project)
        int minSdkVersion = -1
        if (type == ProjectType.AndroidAppProject || type == ProjectType.AndroidLibProject) {
            if (hasFlavors(project)) {
                ProductFlavor productFlavor = getProductFlavors(project).get(flavor)
                if (productFlavor != null) {
                    minSdkVersion = productFlavor.minSdkVersion.apiLevel
                }
            }
            if (minSdkVersion < 0) {
                minSdkVersion = project.android.defaultConfig.minSdkVersion.apiLevel
            }
        }
        return minSdkVersion
    }

    public static int getTargetSdkVersion(Project project, String flavor) {
        ProjectType type = getSubProjectType(project)
        int targetSdkVersion = -1
        if (type == ProjectType.AndroidAppProject || type == ProjectType.AndroidLibProject) {
            if (hasFlavors(project)) {
                ProductFlavor productFlavor = getProductFlavors(project).get(flavor)
                if (productFlavor != null) {
                    targetSdkVersion = productFlavor.targetSdkVersion.apiLevel
                }
            }
            if (targetSdkVersion < 0) {
                targetSdkVersion = project.android.defaultConfig.targetSdkVersion.apiLevel
            }
        }
        return targetSdkVersion
    }

    public static Set<String> getProjectSrcSet(Project project, String flavorVariant) {
        Set<String> srcSet = new HashSet<>()
        switch (getSubProjectType(project)) {
            case ProjectType.AndroidAppProject:
            case ProjectType.AndroidLibProject:
                for (File srcDir :
                        project.android.sourceSets.getByName(flavorVariant).java.srcDirs) {
                    if (srcDir.exists()) {
                        srcSet.add(FileUtil.getDirPathDiff(project.projectDir, srcDir).substring(1))
                    }
                }
                break
            case ProjectType.JavaLibProject:
                for (File srcDir : project.sourceSets.main.java.srcDirs) {
                    if (srcDir.exists()) {
                        srcSet.add(FileUtil.getDirPathDiff(project.projectDir, srcDir).substring(1))
                    }
                }
                break
            default:
                throw new IllegalArgumentException(
                        "sub project must be android library/application module")
        }
        return srcSet
    }

    /**
     * Get the main res dir canonical name, buck's android_resource only accept one dir.
     * return null if the res dir doesn't exist.
     * */
    public static String getProjectResDir(Project project, String flavorVariant) {
        switch (getSubProjectType(project)) {
            case ProjectType.AndroidAppProject:
            case ProjectType.AndroidLibProject:
                File resDir =
                        (File) project.android.sourceSets.getByName(flavorVariant).res.srcDirs[0]
                if (resDir.exists()) {
                    return FileUtil.getDirPathDiff(project.projectDir, resDir).substring(1)
                } else {
                    return null
                }
            case ProjectType.JavaLibProject:
            default:
                throw new IllegalArgumentException(
                        "sub project must be android library/application module")
        }
    }

    /**
     * Get the main assets dir canonical name, buck's android_resource only accept one dir.
     * return null if the assets dir doesn't exist.
     * */
    public static String getProjectAssetsDir(Project project, String flavorVariant) {
        switch (getSubProjectType(project)) {
            case ProjectType.AndroidAppProject:
            case ProjectType.AndroidLibProject:
                File assetsDir = (File) project.android.sourceSets.
                        getByName(flavorVariant).assets.srcDirs[0]
                if (assetsDir.exists()) {
                    return FileUtil.getDirPathDiff(project.projectDir, assetsDir).substring(1)
                } else {
                    return null
                }
            case ProjectType.JavaLibProject:
            default:
                throw new IllegalArgumentException(
                        "sub project must be android library/application module")
        }
    }

    /**
     * Get the main manifest file path.
     * return null if the manifest file doesn't exist.
     * */
    public static String getProjectManifestFile(Project project, String flavorVariant) {
        switch (getSubProjectType(project)) {
            case ProjectType.AndroidAppProject:
            case ProjectType.AndroidLibProject:
                File manifestFile = (File) project.android.sourceSets.
                        getByName(flavorVariant).manifest.srcFile
                if (manifestFile.exists()) {
                    return FileUtil.getDirPathDiff(project.projectDir, manifestFile).substring(1)
                } else {
                    return null
                }
            case ProjectType.JavaLibProject:
            default:
                throw new IllegalArgumentException(
                        "sub project must be android library/application module")
        }
    }

    /**
     * Get the main jniLibs dir path. Usually you can put your jni libs inside your android app
     * module, android library module is ok, but doesn't work with java library module.
     *
     * return null if the jniLibs dir doesn't exist.
     * */
    public static String getProjectJniLibsDir(Project project, String flavorVariant) {
        switch (getSubProjectType(project)) {
            case ProjectType.AndroidAppProject:
            case ProjectType.AndroidLibProject:
                File jniLibsDir = (File) project.android.sourceSets
                        .getByName(flavorVariant).jniLibs.srcDirs[0]
                if (jniLibsDir.exists()) {
                    return FileUtil.getDirPathDiff(project.projectDir, jniLibsDir).substring(1)
                } else {
                    return null
                }
            case ProjectType.JavaLibProject:
            default:
                throw new IllegalArgumentException(
                        "sub project must be android library/application module")
        }
    }

    public static boolean getMultiDexEnabled(Project project) {
        return project.android.defaultConfig.multiDexEnabled
    }

    public static List<String> getPresentResCanonicalNames(
            Project module, String flavor, String variant
    ) {
        List<String> resNames = new ArrayList<>()
        if (isProjectResDirPresent(module, "main")) {
            resNames.add("res_main")
        }
        if (!StringUtil.isEmpty(flavor)) {
            if (isProjectResDirPresent(module, flavor)) {
                resNames.add("res_${flavor}")
            }

            if (!StringUtil.isEmpty(variant) &&
                    isProjectResDirPresent(module, flavor + variant.capitalize())) {
                resNames.add("res_${flavor}_${variant}")
            }
        } else if (!StringUtil.isEmpty(variant)) {
            if (isProjectResDirPresent(module, variant)) {
                resNames.add("res_${variant}")
            }
        }
        return resNames
    }

    public static boolean isProjectResDirPresent(Project project, String flavorVariant) {
        switch (getSubProjectType(project)) {
            case ProjectType.AndroidAppProject:
            case ProjectType.AndroidLibProject:
                try {
                    File resDir =
                            (File) project.android.sourceSets.getByName(flavorVariant).res.srcDirs[0]
                    return resDir.exists()
                } catch (Exception e) {
                    logger.warn e.message
                }
                return false
            case ProjectType.JavaLibProject:
            default:
                return false
        }
    }
}
