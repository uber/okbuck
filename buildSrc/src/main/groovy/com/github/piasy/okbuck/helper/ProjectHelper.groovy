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
import com.android.build.gradle.internal.dsl.SigningConfig
import com.android.builder.model.ClassField
import com.github.piasy.okbuck.rules.KeystoreRule
import org.apache.commons.io.IOUtils
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.plugins.JavaPlugin

/**
 * helper class for android project.
 * */
public final class ProjectHelper {
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
        for (Plugin plugin : project.plugins) {
            if (plugin instanceof AppPlugin) {
                return ProjectType.AndroidAppProject
            } else if (plugin instanceof LibraryPlugin) {
                return ProjectType.AndroidLibProject
            } else if (plugin instanceof JavaPlugin) {
                return ProjectType.JavaLibProject
            }
        }

        return ProjectType.Unknown
    }

    /**
     * get path diff between (sub) project and root project
     *
     * @return path diff, with prefix {@code File.separator}
     * */
    public static String getPathDiff(Project rootProject, Project project) {
        return project.projectDir.absolutePath.substring(
                rootProject.projectDir.absolutePath.length())
    }

    /**
     * get path diff between (sub) dir and root dir
     *
     * @return path diff, with prefix {@code File.separator}
     * */
    public static String getPathDiff(File rootDir, File dir) {
        return dir.absolutePath.substring(rootDir.absolutePath.length())
    }

    public static List<String> getDefaultConfigBuildConfigField(Project project) {
        println "get ${project.name}'s buildConfigField:"
        List<String> ret = new ArrayList<>()
        ProjectType type = getSubProjectType(project)
        if (type == ProjectType.AndroidAppProject || type == ProjectType.AndroidLibProject) {
            try {
                project.extensions.getByName("android").metaPropertyValues.each { prop ->
                    if ("defaultConfig".equals(prop.name) && ProductFlavor.class.isAssignableFrom(
                            prop.type)) {
                        ProductFlavor flavor = (ProductFlavor) prop.value
                        for (ClassField classField : flavor.buildConfigFields.values()) {
                            ret.add("${classField.type} ${classField.name} = ${classField.value}")
                        }
                    }
                }
            } catch (Exception e) {
                println "get ${project.name}'s buildConfigField fail!"
            }
        }
        return ret
    }

    public static KeystoreRule createKeystoreRule(
            Project project, String signConfigName, File dir
    ) {
        if (!dir.exists()) {
            dir.mkdirs()
        }
        try {
            for (PropertyValue prop : project.extensions.getByName("android").metaPropertyValues) {
                if ("signingConfigs".equals(prop.name) && NamedDomainObjectContainer.class.
                        isAssignableFrom(prop.type)) {
                    NamedDomainObjectContainer<SigningConfig> signConfig = (NamedDomainObjectContainer<SigningConfig>) prop.value
                    SigningConfig config
                    if (signConfig.size() == 1) {
                        config = signConfig.getAt(0)
                    } else {
                        config = signConfig.getByName(signConfigName)
                    }
                    IOUtils.copy(new FileInputStream(config.getStoreFile()),
                            new FileOutputStream(new File(
                                    dir.absolutePath + File.separator +
                                            project.name +
                                            ".keystore")))

                    PrintWriter writer = new PrintWriter(new FileOutputStream(new File(
                            "${dir.absolutePath}${File.separator}${project.name}.keystore.properties")))
                    writer.println("key.store=${project.name}.keystore")
                    writer.println("key.alias=${config.getKeyAlias()}")
                    writer.println("key.store.password=${config.getStorePassword()}")
                    writer.println("key.alias.password=${config.getKeyPassword()}")
                    writer.close()

                    return new KeystoreRule(Arrays.asList("PUBLIC"), "${project.name}.keystore",
                            "${project.name}.keystore.properties")
                }
            }
        } catch (UnknownDomainObjectException e) {
            throw new IllegalStateException(
                    "Can not figure out sign config, please make sure you have only one sign config in your build.gradle, or set signConfigName in okbuck dsl.")
        } catch (Exception e) {
            e.printStackTrace()
            throw new IllegalStateException("get ${project.name}'s sign config fail!")
        }
        throw new IllegalStateException("get ${project.name}'s sign config fail!")
    }

    public static Set<String> getProjectMainSrcSet(Project project) {
        Set<String> srcSet = new HashSet<>()
        switch (getSubProjectType(project)) {
            case ProjectType.AndroidAppProject:
            case ProjectType.AndroidLibProject:
                for (File srcDir : project.android.sourceSets.main.java.srcDirs) {
                    if (srcDir.exists()) {
                        srcSet.add(getPathDiff(project.projectDir, srcDir).substring(1))
                    }
                }
                break
            case ProjectType.JavaLibProject:
                for (File srcDir : project.sourceSets.main.java.srcDirs) {
                    if (srcDir.exists()) {
                        srcSet.add(getPathDiff(project.projectDir, srcDir).substring(1))
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
    public static String getProjectMainResDir(Project project) {
        switch (getSubProjectType(project)) {
            case ProjectType.AndroidAppProject:
            case ProjectType.AndroidLibProject:
                File resDir = (File) project.android.sourceSets.main.res.srcDirs[0]
                if (resDir.exists()) {
                    return getPathDiff(project.projectDir, resDir).substring(1)
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
    public static String getProjectMainAssetsDir(Project project) {
        switch (getSubProjectType(project)) {
            case ProjectType.AndroidAppProject:
            case ProjectType.AndroidLibProject:
                File assetsDir = (File) project.android.sourceSets.main.assets.srcDirs[0]
                if (assetsDir.exists()) {
                    return getPathDiff(project.projectDir, assetsDir).substring(1)
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
    public static String getProjectMainManifestFile(Project project) {
        switch (getSubProjectType(project)) {
            case ProjectType.AndroidAppProject:
            case ProjectType.AndroidLibProject:
                File manifestFile = (File) project.android.sourceSets.main.manifest.srcFile
                if (manifestFile.exists()) {
                    return getPathDiff(project.projectDir, manifestFile).substring(1)
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
    public static String getProjectMainJniLibsDir(Project project) {
        switch (getSubProjectType(project)) {
            case ProjectType.AndroidAppProject:
            case ProjectType.AndroidLibProject:
                File jniLibsDir = (File) project.android.sourceSets.main.jniLibs.srcDirs[0]
                if (jniLibsDir.exists()) {
                    return getPathDiff(project.projectDir, jniLibsDir).substring(1)
                } else {
                    return null
                }
            case ProjectType.JavaLibProject:
            default:
                throw new IllegalArgumentException(
                        "sub project must be android library/application module")
        }
    }

}