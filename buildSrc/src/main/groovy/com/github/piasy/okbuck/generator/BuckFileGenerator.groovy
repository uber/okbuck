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

package com.github.piasy.okbuck.generator

import com.android.build.gradle.internal.dsl.ProductFlavor
import com.android.build.gradle.internal.dsl.SigningConfig
import com.android.builder.model.ClassField
import com.github.piasy.okbuck.analyzer.DependencyAnalyzer
import com.github.piasy.okbuck.helper.ProjectHelper
import org.apache.commons.io.IOUtils
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException

class BuckFileGenerator {
    private final Project mRootProject
    private final DependencyAnalyzer mDependencyAnalyzer
    private final File mThirdPartyLibsDir
    private final Map<String, String> mResPackages
    private final boolean mOverwrite
    private final String mKeystoreDir
    private final String mSignConfigName
    private final String mBuildVariant

    public BuckFileGenerator(
            Project rootProject, DependencyAnalyzer dependencyAnalyzer, File thirdPartyLibsDir,
            Map<String, String> resPackages, boolean overwrite, String keystoreDir,
            String signConfigName, String buildVariant
    ) {
        mRootProject = rootProject
        mDependencyAnalyzer = dependencyAnalyzer
        mThirdPartyLibsDir = thirdPartyLibsDir
        mResPackages = resPackages
        mOverwrite = overwrite
        mKeystoreDir = keystoreDir
        mSignConfigName = signConfigName
        mBuildVariant = buildVariant
    }

    /**
     * generate BUCK file for all sub project, including: copy third party libraries; generate BUCK
     * file for them; generate BUCK file for each sub project; generate signing config BUCK.
     * */
    public void generate() {
        // copy third party libs jar/aar and apt jar/aar, then generate BUCK file for them
        mRootProject.subprojects { project ->
            File subProjectLibsDir = new File(
                    mThirdPartyLibsDir.absolutePath + ProjectHelper.getPathDiff(mRootProject,
                            project))
            if (!subProjectLibsDir.exists()) {
                subProjectLibsDir.mkdirs()
            }
            copyDependencies(subProjectLibsDir,
                    mDependencyAnalyzer.allSubProjectsExternalDependenciesExcluded.get(
                            project.name))

            int type = ProjectHelper.getSubProjectType(project)
            switch (type) {
                case ProjectHelper.JAVA_LIB_PROJECT:
                    generateJavaProjectThirdPartyLibsBUCK(subProjectLibsDir)
                    break
                case ProjectHelper.ANDROID_LIB_PROJECT:
                case ProjectHelper.ANDROID_APP_PROJECT:
                    generateAndroidProjectThirdPartyLibsBUCK(subProjectLibsDir)
                    break
            }

            File aptDir = new File(
                    "${mThirdPartyLibsDir.absolutePath}${File.separator}annotation_processor_deps")
            if (!aptDir.exists()) {
                aptDir.mkdirs()
            }
            copyDependencies(aptDir,
                    mDependencyAnalyzer.allSubProjectsAptDependencies.get(project.name))
            generateAndroidProjectThirdPartyLibsBUCK(aptDir)
        }

        // create BUCK file for each sub project
        mRootProject.subprojects { project ->
            File buck = new File("${project.projectDir.absolutePath}${File.separator}BUCK")
            if (buck.exists() && !mOverwrite) {
                throw new IllegalStateException(
                        "sub project ${project.name}'s BUCK file already exist,  set overwrite property to true to overwrite existing file.")
            } else {
                int type = ProjectHelper.getSubProjectType(project)
                switch (type) {
                    case ProjectHelper.JAVA_LIB_PROJECT:
                        // java library can only depend on jar, never aar
                        generateJavaLibSubProjectBUCK(project,
                                mDependencyAnalyzer.allSubProjectsInternalDependenciesExcluded.get(
                                        project.name),
                                mDependencyAnalyzer.annotationProcessors.get(project.name))
                        break
                    case ProjectHelper.ANDROID_LIB_PROJECT:
                        if (mResPackages.get(project.name) == null ||
                                mResPackages.get(project.name).isEmpty()) {
                            throw new IllegalArgumentException(
                                    "resPackages entry for ${project.name} must be set")
                        } else {
                            generateAndroidLibSubProjectBUCK(project,
                                    mResPackages.get(project.name))
                        }
                        break
                    case ProjectHelper.ANDROID_APP_PROJECT:
                        if (mResPackages.get(project.name) == null ||
                                mResPackages.get(project.name).isEmpty()) {
                            throw new IllegalArgumentException(
                                    "resPackages entry for ${project.name} must be set")
                        } else {
                            generateAndroidAppSubProjectBUCK(project,
                                    mResPackages.get(project.name), mKeystoreDir, mSignConfigName,
                                    mBuildVariant)
                        }
                        break
                }
            }
        }
    }

    private static copyDependencies(File dir, Set<File> depsExcluded) {
        for (File dep : depsExcluded) {
            println "copying ${dep.absolutePath} into .okbuck/${dir.name}"
            IOUtils.copy(new FileInputStream(dep), new FileOutputStream(
                    new File(dir.absolutePath + File.separator + dep.name)))
        }
    }

    private static void generateJavaProjectThirdPartyLibsBUCK(File thirdPartyLibsPath) {
        println "generating third-party-libs BUCK in .okbuck/${thirdPartyLibsPath.name}"
        PrintWriter printWriter = new PrintWriter(
                new FileOutputStream("${thirdPartyLibsPath.absolutePath}${File.separator}BUCK"))
        printWriter.println("import re")
        printWriter.println()

        printWriter.println("jar_deps = []")
        printWriter.println("for jarfile in glob(['*.jar']):")
        printWriter.println("\tname = 'jars__' + re.sub(r'^.*/([^/]+)\\.jar\$', r'\\1', jarfile)")
        printWriter.println("\tjar_deps.append(':' + name)")
        printWriter.println("\tprebuilt_jar(")
        printWriter.println("\t\tname = name,")
        printWriter.println("\t\tbinary_jar = jarfile,")
        printWriter.println("\t)")
        printWriter.println()

        printWriter.println("java_library(")
        printWriter.println("\tname = 'all-jars',")
        printWriter.println("\texported_deps = jar_deps,")
        printWriter.println("\tvisibility = [")
        printWriter.println("\t\t'PUBLIC',")
        printWriter.println("\t],")
        printWriter.println(")")
        printWriter.println()

        printWriter.close()
    }

    private static void generateAndroidProjectThirdPartyLibsBUCK(File thirdPartyLibsPath) {
        println "generating third-party-libs BUCK in .okbuck/${thirdPartyLibsPath.name}"
        PrintWriter printWriter = new PrintWriter(
                new FileOutputStream("${thirdPartyLibsPath.absolutePath}${File.separator}BUCK"))
        printWriter.println("import re")
        printWriter.println()

        printWriter.println("jar_deps = []")
        printWriter.println("for jarfile in glob(['*.jar']):")
        printWriter.println("\tname = 'jars__' + re.sub(r'^.*/([^/]+)\\.jar\$', r'\\1', jarfile)")
        printWriter.println("\tjar_deps.append(':' + name)")
        printWriter.println("\tprebuilt_jar(")
        printWriter.println("\t\tname = name,")
        printWriter.println("\t\tbinary_jar = jarfile,")
        printWriter.println("\t)")
        printWriter.println()

        printWriter.println("android_library(")
        printWriter.println("\tname = 'all-jars',")
        printWriter.println("\texported_deps = jar_deps,")
        printWriter.println("\tvisibility = [")
        printWriter.println("\t\t'PUBLIC',")
        printWriter.println("\t],")
        printWriter.println(")")
        printWriter.println()

        printWriter.println("aar_deps = []")
        printWriter.println("for aarfile in glob(['*.aar']):")
        printWriter.println("\tname = 'aar__' + re.sub(r'^.*/([^/]+)\\.aar\$', r'\\1', aarfile)")
        printWriter.println("\taar_deps.append(':' + name)")
        printWriter.println("\tandroid_prebuilt_aar(")
        printWriter.println("\t\tname = name,")
        printWriter.println("\t\taar = aarfile,")
        printWriter.println("\t\tvisibility = ['PUBLIC',],")
        printWriter.println("\t)")
        printWriter.println()

        printWriter.println("android_library(")
        printWriter.println("\tname = 'all-aars',")
        printWriter.println("\texported_deps = aar_deps,")
        printWriter.println("\tvisibility = [")
        printWriter.println("\t\t'PUBLIC',")
        printWriter.println("\t],")
        printWriter.println(")")
        printWriter.println()

        printWriter.close()
    }

    private void generateAndroidAppSubProjectBUCK(
            Project project, String resPackage, String keystoreDir, String signConfigName,
            String buildVariant
    ) {
        println "generating sub project ${project.name}'s BUCK"
        PrintWriter printWriter = new PrintWriter(
                new FileOutputStream("${project.projectDir.absolutePath}${File.separator}BUCK"))

        generateSignConfigsRule(project,
                new File(project.rootProject.projectDir.absolutePath + File.separator +
                        keystoreDir + File.separator + project.name), signConfigName)

        generateAppLibCommonPart(project, printWriter, resPackage)

        printWriter.println("android_binary(")
        printWriter.println("\tname = 'bin',")
        printWriter.println("\tmanifest = 'src/main/AndroidManifest.xml',")
        printWriter.println(
                "\tkeystore = '//${keystoreDir}/${project.name}:${project.name}_keystore',")
        // not included until proguard support
        //printWriter.println("\tpackage_type = '${buildVariant}',")
        printWriter.println("\tdeps = [")
        printWriter.println("\t\t':res',")
        printWriter.println("\t\t':src',")
        printWriter.println("\t],")
        printWriter.println(")")
        printWriter.println()

        printWriter.println("project_config(")
        printWriter.println("\tsrc_target = ':bin',")
        printWriter.println("\tsrc_roots = ['src/main/java'],")
        printWriter.println(")")
        printWriter.println()
        printWriter.close()
    }

    private void generateAndroidLibSubProjectBUCK(Project project, String resPackage) {
        println "generating sub project ${project.name}'s BUCK"
        PrintWriter printWriter = new PrintWriter(
                new FileOutputStream("${project.projectDir.absolutePath}${File.separator}BUCK"))

        generateAppLibCommonPart(project, printWriter, resPackage)

        printWriter.println("project_config(")
        printWriter.println(
                "\tsrc_target = '/${ProjectHelper.getPathDiff(mRootProject, project)}:src',")
        printWriter.println("\tsrc_roots = ['src/main/java'],")
        printWriter.println(")")
        printWriter.println()

        printWriter.close()
    }

    private void generateJavaLibSubProjectBUCK(
            Project project, Set<Project> internalDeps, Set<String> annotationProcessors
    ) {
        println "generating sub project ${project.name}'s BUCK"
        PrintWriter printWriter = new PrintWriter(
                new FileOutputStream("${project.projectDir.absolutePath}${File.separator}BUCK"))
        printWriter.println("java_library(")
        printWriter.println("\tname = 'src',")
        printWriter.println("\tsrcs = glob(['src/main/java/**/*.java']),")
        printWriter.println("\tdeps = [")
        printWriter.println(
                "\t\t'//.okbuck${ProjectHelper.getPathDiff(mRootProject, project)}:all-jars',")
        for (Project internalDep : internalDeps) {
            printWriter.println(
                    "\t\t'/${ProjectHelper.getPathDiff(mRootProject, internalDep)}:src',")
        }
        printWriter.println("\t],")
        printWriter.println("\texported_deps = [")
        printWriter.println(
                "\t\t'//.okbuck${ProjectHelper.getPathDiff(mRootProject, project)}:all-jars',")
        for (Project internalDep : internalDeps) {
            printWriter.println(
                    "\t\t'/${ProjectHelper.getPathDiff(mRootProject, internalDep)}:src',")
        }
        printWriter.println("\t],")
        printWriter.println("\tvisibility = ['PUBLIC'],")

        printWriter.println("\tannotation_processors = [")
        for (String processor : annotationProcessors) {
            printWriter.println("\t\t'${processor}',")
        }
        printWriter.println("\t],")
        printWriter.println("\tannotation_processor_deps = [")
        for (Project internalDep : internalDeps) {
            printWriter.println(
                    "\t\t'/${ProjectHelper.getPathDiff(mRootProject, internalDep)}:src',")
        }
        printWriter.println(
                "\t\t'//.okbuck${ProjectHelper.getPathDiff(mRootProject, project)}:all-jars',")
        printWriter.println("\t\t'//.okbuck/annotation_processor_deps:all-jars',")
        printWriter.println("\t],")

        printWriter.println(")")
        printWriter.println()

        printWriter.println("project_config(")
        printWriter.println(
                "\tsrc_target = '/${ProjectHelper.getPathDiff(mRootProject, project)}:src',")
        printWriter.println("\tsrc_roots = ['src/main/java'],")
        printWriter.println(")")

        printWriter.close()
    }

    private void generateSignConfigsRule(Project project, File dir, String signConfigName) {
        if (!dir.exists()) {
            dir.mkdirs()
        }
        try {
            project.extensions.getByName("android").metaPropertyValues.each { prop ->
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
                                    "${dir.absolutePath}${File.separator}${project.name}.keystore")))
                    PrintWriter writer = new PrintWriter(new FileOutputStream(new File(
                            "${dir.absolutePath}${File.separator}${project.name}.keystore.properties")))
                    writer.println("key.store=${project.name}.keystore")
                    writer.println("key.alias=${config.getKeyAlias()}")
                    writer.println("key.store.password=${config.getStorePassword()}")
                    writer.println("key.alias.password=${config.getKeyPassword()}")
                    writer.close()

                    writer = new PrintWriter(new FileOutputStream(new File(
                            "${dir.absolutePath}${File.separator}BUCK")))
                    writer.println("keystore(")
                    writer.println("\tname = '${project.name}_keystore',")
                    writer.println("\tstore = '${project.name}.keystore',")
                    writer.println("\tproperties = '${project.name}.keystore.properties',")
                    writer.println(
                            "\tvisibility = ['/${ProjectHelper.getPathDiff(mRootProject, project)}:bin'],")
                    writer.println(")")
                    writer.close()
                }
            }
        } catch (UnknownDomainObjectException e) {
            throw new IllegalStateException(
                    "Can not figure out sign config, please make sure you have only one sign config in your build.gradle, or set signConfigName in okbuck dsl.")
        } catch (Exception e) {
            e.printStackTrace()
            throw new IllegalStateException("get ${project.name}'s sign config fail!")
        }
    }

    private void generateAppLibCommonPart(
            Project project, PrintWriter printWriter, String resPackage
    ) {
        File resDir = new File("${project.projectDir.absolutePath}/src/main/res")
        boolean resExists = resDir.exists()
        if (resExists) {
            generateResRule(printWriter, project, resPackage,
                    mDependencyAnalyzer.allSubProjectsInternalDependencies.get(project.name),
                    mDependencyAnalyzer.allSubProjectsExternalDependencies.get(project.name))
        }

        generateBuildConfigRule(printWriter, resPackage, project)

        printWriter.println("android_library(")
        printWriter.println("\tname = 'src',")
        printWriter.println("\tsrcs = glob(['src/main/java/**/*.java']),")
        printWriter.println("\tdeps = [")
        if (resExists) {
            printWriter.println("\t\t':res',")
        }
        printWriter.println("\t\t':build_config',")
        printWriter.println()
        for (Project internalDep :
                mDependencyAnalyzer.allSubProjectsInternalDependencies.get(project.name)) {
            File internalDepResDir = new File("${internalDep.projectDir.absolutePath}/src/main/res")
            if (includeInternalSubProjectResDep(internalDep) && internalDepResDir.exists()) {
                printWriter.println(
                        "\t\t'/${ProjectHelper.getPathDiff(mRootProject, internalDep)}:res',")
            }
        }
        for (Project internalDep :
                mDependencyAnalyzer.allSubProjectsInternalDependenciesExcluded.get(project.name)) {
            printWriter.println(
                    "\t\t'/${ProjectHelper.getPathDiff(mRootProject, internalDep)}:src',")
        }
        printWriter.println()
        for (File externalDep :
                mDependencyAnalyzer.allSubProjectsExternalDependencies.get(project.name)) {
            if (externalDep.name.endsWith(".aar")) {
                printWriter.println(
                        "\t\t'//.okbuck${getSourceDependsProjectName(externalDep)}:aar__${externalDep.name}',")
            }
        }
        printWriter.println(
                "\t\t'//.okbuck${ProjectHelper.getPathDiff(mRootProject, project)}:all-jars',")
        printWriter.println("\t],")
        printWriter.println("\texported_deps = [")
        for (Project internalDep :
                mDependencyAnalyzer.allSubProjectsInternalDependenciesExcluded.get(project.name)) {
            printWriter.println(
                    "\t\t'/${ProjectHelper.getPathDiff(mRootProject, internalDep)}:src',")
        }
        printWriter.println()
        printWriter.println(
                "\t\t'//.okbuck${ProjectHelper.getPathDiff(mRootProject, project)}:all-jars',")
        printWriter.println("\t],")
        printWriter.println("\tvisibility = ['PUBLIC'],")

        generateAnnotationProcessorPart(printWriter,
                mDependencyAnalyzer.annotationProcessors.get(project.name), project)

        printWriter.println(")")
        printWriter.println()
    }

    private void generateBuildConfigRule(
            PrintWriter printWriter, String resPackage, Project project
    ) {
        printWriter.println("android_build_config(")
        printWriter.println("\tname = 'build_config',")
        printWriter.println("\tpackage = '${resPackage}',")
        printWriter.println("\tvalues = [")
        List<String> buildConfigFields = getDefaultConfigBuildConfigField(project)
        for (String field : buildConfigFields) {
            printWriter.println("\t\t'${field}',")
        }
        printWriter.println("\t],")
        printWriter.println(
                "\tvisibility = ['/${ProjectHelper.getPathDiff(mRootProject, project)}:src'],")
        printWriter.println(")")
        printWriter.println()
    }

    private static List<String> getDefaultConfigBuildConfigField(Project project) {
        println "get ${project.name}'s buildConfigField:"
        List<String> ret = new ArrayList<>()
        int type = ProjectHelper.getSubProjectType(project)
        if (type == ProjectHelper.ANDROID_LIB_PROJECT ||
                type ==
                ProjectHelper.ANDROID_APP_PROJECT) {
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

    private void generateResRule(
            PrintWriter printWriter, Project project, String resPackage, Set<Project> internalDeps,
            Set<File> externalDeps
    ) {
        printWriter.println("android_resource(")
        printWriter.println("\tname = 'res',")
        printWriter.println("\tres = 'src/main/res',")
        File assets = new File("${project.projectDir.absolutePath}/src/main/assets")
        if (assets.exists()) {
            println "sub project ${project.name}'s assets exist include it"
            printWriter.println("\tassets = 'src/main/assets',")
        } else {
            println "sub project ${project.name}'s assets not exist"
        }
        printWriter.println("\tpackage = '${resPackage}',")
        printWriter.println("\tdeps = [")
        for (Project internalDep : internalDeps) {
            File internalDepResDir = new File("${internalDep.projectDir.absolutePath}/src/main/res")
            if (includeInternalSubProjectResDep(internalDep) && internalDepResDir.exists()) {
                printWriter.println(
                        "\t\t'/${ProjectHelper.getPathDiff(mRootProject, internalDep)}:res',")
            }
        }
        printWriter.println()
        for (File externalDep : externalDeps) {
            if (externalDep.name.endsWith(".aar")) {
                printWriter.println(
                        "\t\t'//.okbuck${getSourceDependsProjectName(externalDep)}:aar__${externalDep.name}',")
            }
        }
        printWriter.println("\t],")
        printWriter.println("\tvisibility = ['PUBLIC'],")
        printWriter.println(")")
        printWriter.println()
    }

    private String getSourceDependsProjectName(File externalDep) {
        for (Project project : mRootProject.subprojects) {
            if (mDependencyAnalyzer.allSubProjectsExternalDependenciesExcluded.get(project.name).
                    contains(externalDep)) {
                return ProjectHelper.getPathDiff(mRootProject, project)
            }
        }
        throw new IllegalArgumentException(
                "This external dep ${externalDep.absolutePath} doesn't contained by any sub project")
    }

    private void generateAnnotationProcessorPart(
            PrintWriter printWriter, Set<String> annotationProcessors, Project project
    ) {
        printWriter.println("\tannotation_processors = [")
        for (String processor : annotationProcessors) {
            printWriter.println("\t\t'${processor}',")
        }
        printWriter.println("\t],")
        printWriter.println("\tannotation_processor_deps = [")
        printWriter.println(
                "\t\t'//.okbuck${ProjectHelper.getPathDiff(mRootProject, project)}:all-jars',")
        printWriter.println(
                "\t\t'//.okbuck${ProjectHelper.getPathDiff(mRootProject, project)}:all-aars',")
        printWriter.println("\t\t'//.okbuck/annotation_processor_deps:all-jars',")
        printWriter.println("\t\t'//.okbuck/annotation_processor_deps:all-aars',")
        printWriter.println("\t],")
    }

    private static boolean includeInternalSubProjectResDep(Project internalDep) {
        int type = ProjectHelper.getSubProjectType(internalDep)
        return type == ProjectHelper.ANDROID_LIB_PROJECT
    }
}