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
import com.android.builder.model.BuildType
import com.android.builder.model.ClassField
import com.github.piasy.okbuck.rules.KeystoreRule
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
     * check whether the project has product flavors and exported non-default configurations.
     * */
    public static boolean exportFlavor(Project project) {
        switch (getSubProjectType(project)) {
            case ProjectType.AndroidAppProject:
                return hasFlavor(project)
            case ProjectType.AndroidLibProject:
                return publishNonDefault(project) && hasFlavor(project)
            default:
                return false
        }
    }

    private static boolean publishNonDefault(Project project) {
        try {
            boolean export = false
            for (PropertyValue prop : project.extensions.getByName("android").metaPropertyValues) {
                if ("publishNonDefault".equals(prop.name)) {
                    export = prop.value
                    break
                }
            }
            return export
        } catch (Exception e) {
            return false
        }
    }

    private static boolean hasFlavor(Project project) {
        try {
            for (PropertyValue prop :
                    project.extensions.getByName("android").metaPropertyValues) {
                if ("productFlavors".equals(prop.name)) {
                    NamedDomainObjectContainer<ProductFlavor> flavors = (NamedDomainObjectContainer<ProductFlavor>) prop.value
                    return !flavors.getAsMap().isEmpty()
                }
            }
        } catch (Exception e) {
            return false
        }

        return false
    }

    /**
     * get product flavors of sub project.
     * */
    public static Map<String, ProductFlavor> getProductFlavors(Project project) {
        if (exportFlavor(project)) {
            try {
                for (PropertyValue prop :
                        project.extensions.getByName("android").metaPropertyValues) {
                    if ("productFlavors".equals(prop.name)) {
                        NamedDomainObjectContainer<ProductFlavor> flavors = (NamedDomainObjectContainer<ProductFlavor>) prop.value
                        return flavors.getAsMap()
                    }
                }
            } catch (Exception e) {
                println "get ${project.name}'s productFlavors fail!"
            }
        } else {
            throw new IllegalArgumentException("Sub project ${project.name} doesn't have flavors")
        }

        return new HashMap<String, ProductFlavor>()
    }

    /**
     * if the dependency is an module dependency, return the module dependency project, null otherwise.
     * */
    public static Project getModuleDependencyProject(Project rootProject, File dependency) {
        for (Project project : rootProject.subprojects) {
            if (dependency.absolutePath.startsWith(project.buildDir.absolutePath)) {
                return project
            }
        }
        return null
    }

    /**
     * check whether the dependency locate inside the libs dir of the project.
     * */
    public static boolean isLocalExternalDependency(Project project, File dependency) {
        return dependency.absolutePath.startsWith(project.projectDir.absolutePath + "/libs/")
    }

    /**
     * contract:
     * android library/app module with flavor: default + flavor + variant, latter overwrite former.
     *                         without flavor: default + release
     * */
    public static List<String> getBuildConfigField(Project project, String flavor, String variant) {
        println "get ${project.name}'s buildConfigField of ${flavor}_${variant}:"
        Map<String, String> buildConfigs = new HashMap<>()
        ProjectType type = getSubProjectType(project)
        if (type == ProjectType.AndroidAppProject || type == ProjectType.AndroidLibProject) {
            try {
                project.extensions.getByName("android").metaPropertyValues.each { prop ->
                    if ("defaultConfig".equals(prop.name) && ProductFlavor.class.isAssignableFrom(
                            prop.type)) {
                        ProductFlavor defaultConfigs = (ProductFlavor) prop.value
                        if (defaultConfigs.applicationId != null) {
                            buildConfigs.put("APPLICATION_ID",
                                    "String APPLICATION_ID = \"${defaultConfigs.applicationId}\"")
                        }
                        buildConfigs.put("BUILD_TYPE", "String BUILD_TYPE = \"${variant}\"")
                        buildConfigs.put("FLAVOR", "String FLAVOR = \"${flavor}\"")
                        if (defaultConfigs.versionCode != null) {
                            buildConfigs.put("VERSION_CODE",
                                    "int VERSION_CODE = ${defaultConfigs.versionCode}")
                        }
                        if (defaultConfigs.versionName != null) {
                            buildConfigs.put("VERSION_NAME",
                                    "String VERSION_NAME = \"${defaultConfigs.versionName}\"")
                        }

                        for (ClassField classField : defaultConfigs.buildConfigFields.values()) {
                            buildConfigs.put(classField.name,
                                    "${classField.type} ${classField.name} = ${classField.value}")
                        }
                    }
                    if ("productFlavors".equals(prop.name)) {
                        if (!"default".equals(flavor)) {
                            for (ProductFlavor productFlavor :
                                    ((NamedDomainObjectContainer<ProductFlavor>) prop.value).
                                            asList()) {
                                if (productFlavor.name.equals(flavor)) {
                                    for (ClassField classField :
                                            productFlavor.buildConfigFields.values()) {
                                        buildConfigs.put(classField.name,
                                                "${classField.type} ${classField.name} = ${classField.value}")
                                    }
                                }
                            }
                        }
                    }
                    if ("buildTypes".equals(prop.name)) {
                        for (BuildType buildType :
                                ((NamedDomainObjectContainer<BuildType>) prop.value).asList()) {
                            if (buildType.name.equals(variant)) {
                                for (ClassField classField :
                                        buildType.buildConfigFields.values()) {
                                    buildConfigs.put(classField.name,
                                            "${classField.type} ${classField.name} = ${classField.value}")
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                println "get ${project.name}'s buildConfigField fail!"
            }
        }
        return buildConfigs.values().asList()
    }

    public static String getVersionName(Project project, String flavor) {
        println "get ${project.name}'s VersionName build config field:"
        ProjectType type = getSubProjectType(project)
        String versionName = ""
        if (type == ProjectType.AndroidAppProject || type == ProjectType.AndroidLibProject) {
            try {
                project.extensions.getByName("android").metaPropertyValues.each { prop ->
                    if ("defaultConfig".equals(prop.name) && ProductFlavor.class.isAssignableFrom(
                            prop.type)) {
                        ProductFlavor defaultConfigs = (ProductFlavor) prop.value
                        if (defaultConfigs.versionName != null) {
                            versionName = defaultConfigs.versionName
                        }
                    }
                    if ("productFlavors".equals(prop.name)) {
                        if (!"default".equals(flavor)) {
                            for (ProductFlavor productFlavor :
                                    ((NamedDomainObjectContainer<ProductFlavor>) prop.value).
                                            asList()) {
                                if (productFlavor.name.equals(flavor)) {
                                    if (productFlavor.versionName != null) {
                                        versionName = productFlavor.versionName
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                println "get ${project.name}'s VersionName build config field: fail!"
            }
        }
        if (StringUtil.isEmpty(versionName)) {
            throw new IllegalStateException("You must specify versionName in your build.gradle")
        }
        return versionName
    }

    public static int getVersionCode(Project project, String flavor) {
        println "get ${project.name}'s VersionCode build config field:"
        ProjectType type = getSubProjectType(project)
        int versionCode = 0
        if (type == ProjectType.AndroidAppProject || type == ProjectType.AndroidLibProject) {
            try {
                project.extensions.getByName("android").metaPropertyValues.each { prop ->
                    if ("defaultConfig".equals(prop.name) && ProductFlavor.class.isAssignableFrom(
                            prop.type)) {
                        ProductFlavor defaultConfigs = (ProductFlavor) prop.value
                        if (defaultConfigs.versionCode != null) {
                            versionCode = defaultConfigs.versionCode
                        }
                    }
                    if ("productFlavors".equals(prop.name)) {
                        if (!"default".equals(flavor)) {
                            for (ProductFlavor productFlavor :
                                    ((NamedDomainObjectContainer<ProductFlavor>) prop.value).
                                            asList()) {
                                if (productFlavor.name.equals(flavor)) {
                                    if (productFlavor.versionCode != null) {
                                        versionCode = productFlavor.versionCode
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                println "get ${project.name}'s VersionCode build config field: fail!"
            }
        }
        return versionCode
    }

    public static int getMinSdkVersion(Project project, String flavor) {
        println "get ${project.name}'s MinSdkVersion build config field:"
        ProjectType type = getSubProjectType(project)
        int minSdkVersion = 0
        if (type == ProjectType.AndroidAppProject || type == ProjectType.AndroidLibProject) {
            try {
                project.extensions.getByName("android").metaPropertyValues.each { prop ->
                    if ("defaultConfig".equals(prop.name) && ProductFlavor.class.isAssignableFrom(
                            prop.type)) {
                        ProductFlavor defaultConfigs = (ProductFlavor) prop.value
                        if (defaultConfigs.minSdkVersion != null) {
                            minSdkVersion = defaultConfigs.minSdkVersion.apiLevel
                        }
                    }
                    if ("productFlavors".equals(prop.name)) {
                        if (!"default".equals(flavor)) {
                            for (ProductFlavor productFlavor :
                                    ((NamedDomainObjectContainer<ProductFlavor>) prop.value).
                                            asList()) {
                                if (productFlavor.name.equals(flavor)) {
                                    if (productFlavor.minSdkVersion != null) {
                                        minSdkVersion = productFlavor.minSdkVersion.apiLevel
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                println "get ${project.name}'s MinSdkVersion build config field: fail!"
            }
        }
        return minSdkVersion
    }

    public static int getTargetSdkVersion(Project project, String flavor) {
        println "get ${project.name}'s TargetSdkVersion build config field:"
        ProjectType type = getSubProjectType(project)
        int targetSdkVersion = 0
        if (type == ProjectType.AndroidAppProject || type == ProjectType.AndroidLibProject) {
            try {
                project.extensions.getByName("android").metaPropertyValues.each { prop ->
                    if ("defaultConfig".equals(prop.name) && ProductFlavor.class.isAssignableFrom(
                            prop.type)) {
                        ProductFlavor defaultConfigs = (ProductFlavor) prop.value
                        if (defaultConfigs.targetSdkVersion != null) {
                            targetSdkVersion = defaultConfigs.targetSdkVersion.apiLevel
                        }
                    }
                    if ("productFlavors".equals(prop.name)) {
                        if (!"default".equals(flavor)) {
                            for (ProductFlavor productFlavor :
                                    ((NamedDomainObjectContainer<ProductFlavor>) prop.value).
                                            asList()) {
                                if (productFlavor.name.equals(flavor)) {
                                    if (productFlavor.targetSdkVersion != null) {
                                        targetSdkVersion = productFlavor.targetSdkVersion.apiLevel
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                println "get ${project.name}'s TargetSdkVersion build config field: fail!"
            }
        }
        return targetSdkVersion
    }

    public static KeystoreRule createKeystoreRule(
            Project project, String signConfigName, File dir
    ) {
        if (!dir.exists()) {
            dir.mkdirs()
        }
        try {
            for (PropertyValue prop : project.extensions.getByName("android").metaPropertyValues) {
                if ("signingConfigs".equals(prop.name)) {
                    NamedDomainObjectContainer<SigningConfig> signConfig = (NamedDomainObjectContainer<SigningConfig>) prop.value
                    SigningConfig config
                    if (signConfig.size() == 1) {
                        config = signConfig.getAt(0)
                    } else {
                        config = signConfig.getByName(signConfigName)
                    }
                    IOHelper.copy(new FileInputStream(config.getStoreFile()),
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
                File resDir = (File) project.android.sourceSets.getByName(flavorVariant).res.srcDirs[0]
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
                File jniLibsDir = (File) project.android.sourceSets.
                        getByName(flavorVariant).jniLibs.srcDirs[0]
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
        try {
            for (PropertyValue prop : project.extensions.getByName("android").metaPropertyValues) {
                if ("defaultConfig".equals(prop.name) && ProductFlavor.class.isAssignableFrom(
                        prop.type)) {
                    ProductFlavor defaultConfigs = (ProductFlavor) prop.value
                    return defaultConfigs.multiDexEnabled != null && defaultConfigs.multiDexEnabled
                }
            }
        } catch (Exception e) {
            println "get ${project.name}'s MultiDexEnabled fail!"
        }
        return false
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
                File resDir = (File) project.android.sourceSets.getByName(flavorVariant).res.srcDirs[0]
                return resDir.exists()
            case ProjectType.JavaLibProject:
            default:
                return false
        }
    }
}