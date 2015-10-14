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
import com.github.piasy.okbuck.helper.AndroidProjectHelper
import org.apache.commons.io.IOUtils
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException

class BuckFileGenerator {
    private final Project mRootProject
    private final Map<String, Set<File>> mAllSubProjectsExternalDependencies
    private final Map<String, Set<Project>> mAllSubProjectsInternalDependencies
    private final Map<String, Set<File>> mAllSubProjectsAptDependencies
    private final Map<String, Set<String>> mAnnotationProcessors
    private final File mThirdPartyLibsDir
    private final Map<String, String> mResPackages
    private final boolean mOverwrite
    private final String mKeystoreDir
    private final String mSignConfigName
    private final String mBuildVariant

    public BuckFileGenerator(
            Project rootProject, Map<String, Set<File>> allSubProjectsExternalDependencies,
            Map<String, Set<Project>> allSubProjectsInternalDependencies,
            Map<String, Set<File>> allSubProjectsAptDependencies,
            Map<String, Set<String>> annotationProcessors, File thirdPartyLibsDir,
            Map<String, String> resPackages, boolean overwrite, String keystoreDir,
            String signConfigName, String buildVariant
    ) {
        mRootProject = rootProject
        mAllSubProjectsExternalDependencies = allSubProjectsExternalDependencies
        mAllSubProjectsInternalDependencies = allSubProjectsInternalDependencies
        mAllSubProjectsAptDependencies = allSubProjectsAptDependencies
        mAnnotationProcessors = annotationProcessors
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
                    mThirdPartyLibsDir.absolutePath + File.separator + project.name)
            if (!subProjectLibsDir.exists()) {
                subProjectLibsDir.mkdirs()
            }
            copyDependencies(subProjectLibsDir,
                    mAllSubProjectsExternalDependencies.get(project.name))

            int type = AndroidProjectHelper.getSubProjectType(project)
            switch (type) {
                case AndroidProjectHelper.JAVA_LIB_PROJECT:
                    genJavaProjectThirdPartyLibsBUCK(subProjectLibsDir)
                    break
                case AndroidProjectHelper.ANDROID_LIB_PROJECT:
                case AndroidProjectHelper.ANDROID_APP_PROJECT:
                    genAndroidProjectThirdPartyLibsBUCK(subProjectLibsDir)
                    break
            }

            File aptDir = new File(
                    "${mThirdPartyLibsDir.absolutePath}${File.separator}annotation_processor_deps")
            if (!aptDir.exists()) {
                aptDir.mkdirs()
            }
            copyDependencies(aptDir, mAllSubProjectsAptDependencies.get(project.name))
            genAndroidProjectThirdPartyLibsBUCK(aptDir)
        }

        // create BUCK file for each sub project
        mRootProject.subprojects { project ->
            File buck = new File("${project.projectDir.absolutePath}${File.separator}BUCK")
            if (buck.exists() && !mOverwrite) {
                throw new IllegalStateException(
                        "sub project ${project.name}'s BUCK file already exist,  set overwrite property to true to overwrite existing file.")
            } else {
                int type = AndroidProjectHelper.getSubProjectType(project)
                switch (type) {
                    case AndroidProjectHelper.JAVA_LIB_PROJECT:
                        genJavaLibSubProjectBUCK(project,
                                mAllSubProjectsInternalDependencies.get(project.name),
                                mAnnotationProcessors.get(project.name))
                        break
                    case AndroidProjectHelper.ANDROID_LIB_PROJECT:
                        if (mResPackages.get(project.name) == null ||
                                mResPackages.get(project.name).isEmpty()) {
                            throw new IllegalArgumentException(
                                    "resPackages entry for ${project.name} must be set")
                        } else {
                            genAndroidLibSubProjectBUCK(project,
                                    mAllSubProjectsInternalDependencies.get(project.name),
                                    mAllSubProjectsExternalDependencies.get(project.name),
                                    mResPackages.get(project.name),
                                    mAnnotationProcessors.get(project.name))
                        }
                        break
                    case AndroidProjectHelper.ANDROID_APP_PROJECT:
                        if (mResPackages.get(project.name) == null ||
                                mResPackages.get(project.name).isEmpty()) {
                            throw new IllegalArgumentException(
                                    "resPackages entry for ${project.name} must be set")
                        } else {
                            genAndroidAppSubProjectBUCK(project,
                                    mAllSubProjectsInternalDependencies.get(project.name),
                                    mAllSubProjectsExternalDependencies.get(project.name),
                                    mResPackages.get(project.name),
                                    mAnnotationProcessors.get(project.name), mKeystoreDir,
                                    mSignConfigName, mBuildVariant)
                        }
                        break
                }
            }
        }
    }

    private static copyDependencies(File dir, Set<File> deps) {
        for (File dep : deps) {
            println "copying ${dep.absolutePath} into .okbuck/${dir.name}"
            IOUtils.copy(new FileInputStream(dep), new FileOutputStream(
                    new File(dir.absolutePath + File.separator + dep.name)))
        }
    }

    private static void genJavaProjectThirdPartyLibsBUCK(File thirdPartyLibsPath) {
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

    private static void genAndroidProjectThirdPartyLibsBUCK(File thirdPartyLibsPath) {
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

    private void genAndroidAppSubProjectBUCK(
            Project project, Set<Project> internalDeps, Set<File> externalDeps, String resPackage,
            Set<String> annotationProcessors,
            String keystoreDir, String signConfigName, String buildVariant
    ) {
        println "generating sub project ${project.name}'s BUCK"
        PrintWriter printWriter = new PrintWriter(
                new FileOutputStream("${project.projectDir.absolutePath}${File.separator}BUCK"))

        genSignConfigs(project,
                new File(project.rootProject.projectDir.absolutePath + File.separator +
                        keystoreDir + File.separator + project.name), signConfigName)

        File resDir = new File("${project.projectDir.absolutePath}/src/main/res")
        boolean resExists = resDir.exists()
        if (resExists) {
            generateResRule(printWriter, project, resPackage, internalDeps, externalDeps)
        }

        printWriter.println("android_build_config(")
        printWriter.println("\tname = 'build_config',")
        printWriter.println("\tpackage = '${resPackage}',")
        printWriter.println("\tvalues = [")
        List<String> buildConfigFields = getDefaultConfigBuildConfigField(project)
        for (String field : buildConfigFields) {
            printWriter.println("\t\t'${field}',")
        }
        printWriter.println("\t],")
        printWriter.println("\tvisibility = ['//${project.name}:src'],")
        printWriter.println(")")
        printWriter.println()

        printWriter.println("android_library(")
        printWriter.println("\tname = 'src',")
        printWriter.println("\tsrcs = glob(['src/main/java/**/*.java']),")
        printWriter.println("\tdeps = [")
        if (resExists) {
            printWriter.println("\t\t':res',")
        }
        printWriter.println("\t\t':build_config',")
        printWriter.println()
        for (Project internalDep : internalDeps) {
            printWriter.println("\t\t'/${getPathDiff(mRootProject, internalDep)}:src',")
            File internalDepResDir = new File("${internalDep.projectDir.absolutePath}/src/main/res")
            if (includeInternalSubProjectResDep(internalDep) && internalDepResDir.exists()) {
                printWriter.println("\t\t'/${getPathDiff(mRootProject, internalDep)}:res',")
            }
        }
        printWriter.println()
        for (File externalDep : externalDeps) {
            if (externalDep.name.endsWith(".aar")) {
                printWriter.println("\t\t'//.okbuck/${project.name}:aar__${externalDep.name}',")
            }
        }
        printWriter.println("\t\t'//.okbuck/${project.name}:all-jars',")
        printWriter.println("\t],")

        printWriter.println("\tannotation_processors = [")
        for (String processor : annotationProcessors) {
            printWriter.println("\t\t'${processor}',")
        }
        printWriter.println("\t],")
        printWriter.println("\tannotation_processor_deps = [")
        for (Project internalDep : internalDeps) {
            printWriter.println("\t\t'/${getPathDiff(mRootProject, internalDep)}:src',")
        }
        printWriter.println("\t\t'//.okbuck/${project.name}:all-jars',")
        printWriter.println("\t\t'//.okbuck/${project.name}:all-aars',")
        printWriter.println("\t\t'//.okbuck/annotation_processor_deps:all-jars',")
        printWriter.println("\t\t'//.okbuck/annotation_processor_deps:all-aars',")
        printWriter.println("\t],")

        printWriter.println(")")
        printWriter.println()

        printWriter.println("android_binary(")
        printWriter.println("\tname = 'bin',")
        printWriter.println("\tmanifest = 'src/main/AndroidManifest.xml',")
        printWriter.println("\tkeystore = '//${keystoreDir}/${project.name}:${project.name}_keystore',")
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

    private static boolean includeInternalSubProjectResDep(Project internalDep) {
        int type = AndroidProjectHelper.getSubProjectType(internalDep)
        return type == AndroidProjectHelper.ANDROID_LIB_PROJECT
    }

    private static void genSignConfigs(Project project, File dir, String signConfigName) {
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
                    writer.println("\tvisibility = ['//${project.name}:bin'],")
                    writer.println(")")
                    writer.close()
                }
            }
        } catch (UnknownDomainObjectException e) {
            throw new IllegalStateException(
                    "Can not figure out sign config, please make sure you have only one sign config in your build.gradle, or set signConfigName in okbuck dsl.")
        } catch (Exception e) {
            throw new IllegalStateException("get ${project.name}'s sign config fail!")
        }
    }

    private void genAndroidLibSubProjectBUCK(
            Project project, Set<Project> internalDeps, Set<File> externalDeps, String resPackage,
            Set<String> annotationProcessors
    ) {
        println "generating sub project ${project.name}'s BUCK"
        PrintWriter printWriter = new PrintWriter(
                new FileOutputStream("${project.projectDir.absolutePath}${File.separator}BUCK"))
        File resDir = new File("${project.projectDir.absolutePath}/src/main/res")
        boolean resExists = resDir.exists()
        if (resExists) {
            generateResRule(printWriter, project, resPackage, internalDeps, externalDeps)
        }

        printWriter.println("android_build_config(")
        printWriter.println("\tname = 'build_config',")
        printWriter.println("\tpackage = '${resPackage}',")
        printWriter.println("\tvalues = [")
        List<String> buildConfigFields = getDefaultConfigBuildConfigField(project)
        for (String field : buildConfigFields) {
            printWriter.println("\t\t'${field}',")
        }
        printWriter.println("\t],")
        printWriter.println("\tvisibility = ['//${project.name}:src'],")
        printWriter.println(")")
        printWriter.println()

        printWriter.println("android_library(")
        printWriter.println("\tname = 'src',")
        printWriter.println("\tsrcs = glob(['src/main/java/**/*.java']),")
        printWriter.println("\tdeps = [")
        if (resExists) {
            printWriter.println("\t\t':res',")
        }
        printWriter.println("\t\t':build_config',")
        printWriter.println()
        for (Project internalDep : internalDeps) {
            printWriter.println("\t\t'/${getPathDiff(mRootProject, internalDep)}:src',")
            File internalDepResDir = new File("${internalDep.projectDir.absolutePath}/src/main/res")
            if (includeInternalSubProjectResDep(internalDep) && internalDepResDir.exists()) {
                printWriter.println("\t\t'/${getPathDiff(mRootProject, internalDep)}:res',")
            }
        }
        printWriter.println()
        for (File externalDep : externalDeps) {
            if (externalDep.name.endsWith(".aar")) {
                printWriter.println("\t\t'//.okbuck/${project.name}:aar__${externalDep.name}',")
            }
        }
        printWriter.println("\t\t'//.okbuck/${project.name}:all-jars',")
        printWriter.println("\t],")
        printWriter.println("\texported_deps = [")
        for (Project internalDep : internalDeps) {
            printWriter.println("\t\t'/${getPathDiff(mRootProject, internalDep)}:src',")
        }
        printWriter.println()
        for (File externalDep : externalDeps) {
            if (externalDep.name.endsWith(".aar")) {
                printWriter.println("\t\t'//.okbuck/${project.name}:aar__${externalDep.name}',")
            }
        }
        printWriter.println("\t\t'//.okbuck/${project.name}:all-jars',")
        printWriter.println("\t],")
        printWriter.println("\tvisibility = ['PUBLIC'],")

        printWriter.println("\tannotation_processors = [")
        for (String processor : annotationProcessors) {
            printWriter.println("\t\t'${processor}',")
        }
        printWriter.println("\t],")
        printWriter.println("\tannotation_processor_deps = [")
        for (Project internalDep : internalDeps) {
            printWriter.println("\t\t'/${getPathDiff(mRootProject, internalDep)}:src',")
        }
        printWriter.println("\t\t'//.okbuck/${project.name}:all-jars',")
        printWriter.println("\t\t'//.okbuck/${project.name}:all-aars',")
        printWriter.println("\t\t'//.okbuck/annotation_processor_deps:all-jars',")
        printWriter.println("\t\t'//.okbuck/annotation_processor_deps:all-aars',")
        printWriter.println("\t],")

        printWriter.println(")")
        printWriter.println()

        printWriter.println("project_config(")
        printWriter.println("\tsrc_target = '//${project.name}:src',")
        printWriter.println("\tsrc_roots = ['src/main/java'],")
        printWriter.println(")")

        printWriter.close()
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
                printWriter.println("\t\t'/${getPathDiff(mRootProject, internalDep)}:res',")
            }
        }
        printWriter.println()
        for (File externalDep : externalDeps) {
            if (externalDep.name.endsWith(".aar")) {
                printWriter.println("\t\t'//.okbuck/${project.name}:aar__${externalDep.name}',")
            }
        }
        printWriter.println("\t],")
        printWriter.println("\tvisibility = ['PUBLIC'],")
        printWriter.println(")")
        printWriter.println()
    }

    private static List<String> getDefaultConfigBuildConfigField(Project project) {
        println "get ${project.name}'s buildConfigField:"
        List<String> ret = new ArrayList<>()
        int type = AndroidProjectHelper.getSubProjectType(project)
        if (type == AndroidProjectHelper.ANDROID_LIB_PROJECT ||
                type ==
                AndroidProjectHelper.ANDROID_APP_PROJECT) {
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

    private void genJavaLibSubProjectBUCK(
            Project project, Set<Project> internalDeps, Set<String> annotationProcessors
    ) {
        println "generating sub project ${project.name}'s BUCK"
        PrintWriter printWriter = new PrintWriter(
                new FileOutputStream("${project.projectDir.absolutePath}${File.separator}BUCK"))
        printWriter.println("java_library(")
        printWriter.println("\tname = 'src',")
        printWriter.println("\tsrcs = glob(['src/main/java/**/*.java']),")
        printWriter.println("\tdeps = [")
        printWriter.println("\t\t'//.okbuck/${project.name}:all-jars',")
        for (Project internalDep : internalDeps) {
            printWriter.println("\t\t'/${getPathDiff(mRootProject, internalDep)}:src',")
        }
        printWriter.println("\t],")
        printWriter.println("\texported_deps = [")
        printWriter.println("\t\t'//.okbuck/${project.name}:all-jars',")
        for (Project internalDep : internalDeps) {
            printWriter.println("\t\t'/${getPathDiff(mRootProject, internalDep)}:src',")
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
            printWriter.println("\t\t'/${getPathDiff(mRootProject, internalDep)}:src',")
        }
        printWriter.println("\t\t'//.okbuck/${project.name}:all-jars',")
        printWriter.println("\t\t'//.okbuck/annotation_processor_deps:all-jars',")
        printWriter.println("\t],")

        printWriter.println(")")
        printWriter.println()

        printWriter.println("project_config(")
        printWriter.println("\tsrc_target = '//${project.name}:src',")
        printWriter.println("\tsrc_roots = ['src/main/java'],")
        printWriter.println(")")

        printWriter.close()
    }

    private static String getPathDiff(Project rootProject, Project project) {
        return project.projectDir.absolutePath.substring(rootProject.projectDir.absolutePath.length())
    }
}