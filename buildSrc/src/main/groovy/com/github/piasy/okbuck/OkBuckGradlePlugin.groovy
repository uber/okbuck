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

package com.github.piasy.okbuck

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.apache.commons.io.IOUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.UnknownConfigurationException
import org.gradle.api.plugins.JavaPlugin

import java.util.jar.JarEntry
import java.util.jar.JarFile

class OkBuckGradlePlugin implements Plugin<Project> {

    void apply(Project project) {
        project.extensions.create("okbuck", OkBuckExtension)

        project.task('okbuck') << {
            boolean overwrite = project.okbuck.overwrite
            if (overwrite) {
                println "==========>> overwrite mode is toggle on <<=========="
            }
            // create .buckconfig
            File buckConfig = new File(
                    "${project.rootDir.absolutePath}${File.separator}.buckconfig")
            if (buckConfig.exists() && !overwrite) {
                throw new IllegalStateException(
                        ".buckconfig file already exist, set overwrite property to true to overwrite existing file.")
            } else {
                genBuckConfig(project, buckConfig)
            }

            printAllSubProjects(project)

            // get all sub projects compile dependencies, internal(project) or external(maven),
            // then filter internal dependency's external dependencies
            Map<String, Set<File>> allSubProjectsExternalDeps = new HashMap<>()
            Map<String, Set<String>> allSubProjectsInternalDeps = new HashMap<>()
            getAllSubProjectsDeps(project, allSubProjectsExternalDeps,
                    allSubProjectsInternalDeps)
            filterInternalsExternalDeps(project, allSubProjectsExternalDeps,
                    allSubProjectsInternalDeps)
            // get all sub projects apt/provided dependencies, internal(project) or external(maven)
            // TODO handle internal(project) apt dependencies
            Map<String, Set<File>> allSubProjectsAptDeps = new HashMap<>()
            getAllSubProjectsAptDeps(project, allSubProjectsAptDeps, true)

            printDeps(project, allSubProjectsExternalDeps, allSubProjectsInternalDeps,
                    allSubProjectsAptDeps)

            File thirdPartyLibsDir = new File(".okbuck")
            if (thirdPartyLibsDir.exists() && !overwrite) {
                throw new IllegalStateException(
                        "third-party libs dir already exist, set overwrite property to true to overwrite existing file.")
            } else {
                genAllSubProjectsBUCK(project, thirdPartyLibsDir, allSubProjectsExternalDeps,
                        allSubProjectsInternalDeps, allSubProjectsAptDeps, overwrite)
            }
        }
    }

    private static void genAllSubProjectsBUCK(
            Project project, File thirdPartyLibsDir,
            Map<String, Set<File>> allSubProjectsExternalDeps,
            Map<String, Set<String>> allSubProjectsInternalDeps,
            Map<String, Set<File>> allSubProjectsAptDeps,
            boolean overwrite
    ) {
        project.subprojects { prj ->
            File subProjectLibsDir = new File(
                    thirdPartyLibsDir.absolutePath + File.separator + prj.name)
            if (!subProjectLibsDir.exists()) {
                subProjectLibsDir.mkdirs()
            }
            copyDependencies(subProjectLibsDir, allSubProjectsExternalDeps.get(prj.name))
            if (getSubProjectType(prj) == JAVA_LIB_PROJECT) {
                genJavaProjectThirdPartyLibsBUCK(subProjectLibsDir)
            } else {
                genAndroidProjectThirdPartyLibsBUCK(subProjectLibsDir)
            }

            File aptDir = new File(
                    "${thirdPartyLibsDir.absolutePath}${File.separator}annotation_processor_deps")
            if (!aptDir.exists()) {
                aptDir.mkdirs()
            }
            copyDependencies(aptDir, allSubProjectsAptDeps.get(prj.name))
            genAndroidProjectThirdPartyLibsBUCK(aptDir)
        }
        Map<String, Set<String>> annotationProcessors = new HashMap<>()
        extractAnnotationProcessors(project, annotationProcessors)

        // create BUCK file for each sub project
        if (project.okbuck.keystore.isEmpty() || project.okbuck.keystoreProperties.isEmpty() ||
                project.okbuck.resPackages == null) {
            throw new IllegalArgumentException(
                    "keystore, keystoreProperties, and resPackages must be specified.")
        } else {
            Map<String, String> resPackages = project.okbuck.resPackages
            project.subprojects { prj ->
                File buck = new File("${prj.projectDir.absolutePath}${File.separator}BUCK")
                if (buck.exists() && !overwrite) {
                    throw new IllegalStateException(
                            "sub project ${prj.name}'s BUCK file already exist,  set overwrite property to true to overwrite existing file.")
                } else {
                    int type = getSubProjectType(prj)
                    switch (type) {
                        case JAVA_LIB_PROJECT:
                            genJavaLibSubProjectBUCK(prj, allSubProjectsInternalDeps.get(prj.name),
                                    annotationProcessors.get(prj.name))
                            break
                        case ANDROID_LIB_PROJECT:
                            if (resPackages.get(prj.name) == null ||
                                    resPackages.get(prj.name).isEmpty()) {
                                throw new IllegalArgumentException(
                                        "resPackages entry for ${prj.name} must be set")
                            } else {
                                genAndroidLibSubProjectBUCK(prj,
                                        allSubProjectsInternalDeps.get(prj.name),
                                        resPackages.get(prj.name),
                                        annotationProcessors.get(prj.name))
                            }
                            break
                        case ANDROID_APP_PROJECT:
                            if (resPackages.get(prj.name) == null ||
                                    resPackages.get(prj.name).isEmpty()) {
                                throw new IllegalArgumentException(
                                        "resPackages entry for ${prj.name} must be set")
                            } else {
                                genAndroidAppSubProjectBUCK(prj,
                                        allSubProjectsInternalDeps.get(prj.name),
                                        resPackages.get(prj.name),
                                        annotationProcessors.get(prj.name))
                            }
                            break
                    }
                }
            }
        }
    }

    private static void getAllSubProjectsAptDeps(
            Project project, Map<String, Set<File>> allSubProjectsAptDeps, boolean excludeCompile
    ) {
        project.subprojects { prj ->
            Set<File> subProjectCompileDeps = new HashSet<>()
            try {
                prj.configurations.getByName("compile").resolve().each { dependency ->
                    subProjectCompileDeps.add(dependency)
                }
            } catch (UnknownConfigurationException e) {
                println "${prj.name} doesn't contain compile configuration"
            }

            allSubProjectsAptDeps.put(prj.name, new HashSet<File>())
            try {
                prj.configurations.getByName("apt").resolve().each { dependency ->
                    if (!excludeCompile || !subProjectCompileDeps.contains(dependency)) {
                        allSubProjectsAptDeps.get(prj.name).add(dependency)
                    }
                }
            } catch (UnknownConfigurationException e) {
                println "${prj.name} doesn't contain apt configuration"
            }
            try {
                prj.configurations.getByName("provided").resolve().each { dependency ->
                    if (!excludeCompile || !subProjectCompileDeps.contains(dependency)) {
                        allSubProjectsAptDeps.get(prj.name).add(dependency)
                    }
                }
            } catch (UnknownConfigurationException e) {
                println "${prj.name} doesn't contain provided configuration"
            }
        }
    }

    private static void getAllSubProjectsDeps(
            Project project,
            Map<String, Set<File>> allSubProjectsExternalDeps,
            Map<String, Set<String>> allSubProjectsInternalDeps
    ) {
        project.subprojects { prj ->
            // for each sub project
            allSubProjectsExternalDeps.put(prj.name, new HashSet<File>())
            allSubProjectsInternalDeps.put(prj.name, new HashSet<String>())
            prj.configurations.compile.resolve().each { dependency ->
                // for each of its compile dependency, if dep's path start with **another** sub
                // project's build path, it's an internal dependency(project), otherwise, it's an
                // external dependency, whether maven/m2/local jars.
                boolean isProjectDep = false
                String projectDep = ""
                for (Project subProject : project.subprojects) {
                    if (!prj.projectDir.equals(subProject.projectDir) && dependency.absolutePath.
                            startsWith(subProject.buildDir.absolutePath)) {
                        isProjectDep = true
                        projectDep = subProject.name
                        break
                    }
                }
                if (isProjectDep) {
                    println "${prj.name}'s dependency ${dependency.absolutePath} is an internal dependency, sub project: ${projectDep}"
                    allSubProjectsInternalDeps.get(prj.name).add(projectDep)
                } else {
                    println "${prj.name}'s dependency ${dependency.absolutePath} is an external dependency"
                    allSubProjectsExternalDeps.get(prj.name).add(dependency)
                }
            }
        }
    }

    private static void filterInternalsExternalDeps(
            Project project,
            Map<String, Set<File>> allSubProjectsExternalDeps,
            Map<String, Set<String>> allSubProjectsInternalDeps
    ) {
        // filter sub project's internal dependency's external dependencies
        project.subprojects { prj ->
            for (String projectDep : allSubProjectsInternalDeps.get(prj.name)) {
                for (File mavenDep : allSubProjectsExternalDeps.get(projectDep)) {
                    if (allSubProjectsExternalDeps.get(prj.name).contains(mavenDep)) {
                        println "${prj.name}'s dependency ${mavenDep.absolutePath} is contained in ${projectDep}, exclude it"
                        allSubProjectsExternalDeps.get(prj.name).remove(mavenDep)
                    }
                }
            }
        }
    }

    private static void genAndroidAppSubProjectBUCK(
            Project project, Set<String> internalDeps, String resPackage,
            Set<String> annotationProcessors
    ) {
        println "generating sub project ${project.name}'s BUCK"
        PrintWriter printWriter = new PrintWriter(
                new FileOutputStream("${project.projectDir.absolutePath}${File.separator}BUCK"))
        printWriter.println("keystore(")
        printWriter.println("\tname = '${project.name}_keystore',")
        printWriter.println("\tstore = '${project.okbuck.keystore}',")
        printWriter.println("\tproperties = '${project.okbuck.keystoreProperties}',")
        printWriter.println(")")
        printWriter.println()

        printWriter.println("android_resource(")
        printWriter.println("\tname = 'res',")
        printWriter.println("\tres = 'src/main/res',")
        printWriter.println("\tpackage = '${resPackage}',")
        printWriter.println("\tvisibility = ['//${project.name}:src'],")
        printWriter.println(")")
        printWriter.println()

        printWriter.println("android_library(")
        printWriter.println("\tname = 'src',")
        printWriter.println("\tsrcs = glob(['src/main/java/**/*.java']),")
        printWriter.println("\tdeps = [")
        printWriter.println("\t\t':res',")
        for (String dep : internalDeps) {
            printWriter.println("\t\t'//${dep}:src',")
        }
        printWriter.println("\t\t'//.okbuck/${project.name}:all-jars',")
        printWriter.println("\t\t'//.okbuck/${project.name}:all-aars',")
        printWriter.println("\t],")

        printWriter.println("\tannotation_processors = [")
        for (String processor : annotationProcessors) {
            printWriter.println("\t\t'${processor}',")
        }
        printWriter.println("\t],")
        printWriter.println("\tannotation_processor_deps = [")
        for (String dep : internalDeps) {
            printWriter.println("\t\t'//${dep}:src',")
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
        printWriter.println("\tkeystore = ':${project.name}_keystore',")
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

    private static void genAndroidLibSubProjectBUCK(
            Project project, Set<String> internalDeps, String resPackage,
            Set<String> annotationProcessors
    ) {
        println "generating sub project ${project.name}'s BUCK"
        PrintWriter printWriter = new PrintWriter(
                new FileOutputStream("${project.projectDir.absolutePath}${File.separator}BUCK"))
        printWriter.println("android_resource(")
        printWriter.println("\tname = 'res',")
        printWriter.println("\tres = 'src/main/res',")
        printWriter.println("\tpackage = '${resPackage}',")
        printWriter.println("\tvisibility = ['//${project.name}:src'],")
        printWriter.println(")")
        printWriter.println()

        printWriter.println("android_library(")
        printWriter.println("\tname = 'src',")
        printWriter.println("\tsrcs = glob(['src/main/java/**/*.java']),")
        printWriter.println("\tdeps = [")
        printWriter.println("\t\t':res',")
        for (String dep : internalDeps) {
            printWriter.println("\t\t'//${dep}:src',")
        }
        printWriter.println("\t\t'//.okbuck/${project.name}:all-jars',")
        printWriter.println("\t\t'//.okbuck/${project.name}:all-aars',")
        printWriter.println("\t],")
        printWriter.println("\texported_deps = [")
        for (String dep : internalDeps) {
            printWriter.println("\t\t'//${dep}:src',")
        }
        printWriter.println("\t\t'//.okbuck/${project.name}:all-jars',")
        printWriter.println("\t\t'//.okbuck/${project.name}:all-aars',")
        printWriter.println("\t],")
        printWriter.println("\tvisibility = ['PUBLIC'],")

        printWriter.println("\tannotation_processors = [")
        for (String processor : annotationProcessors) {
            printWriter.println("\t\t'${processor}',")
        }
        printWriter.println("\t],")
        printWriter.println("\tannotation_processor_deps = [")
        for (String dep : internalDeps) {
            printWriter.println("\t\t'//${dep}:src',")
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

    private static void genJavaLibSubProjectBUCK(
            Project project, Set<String> internalDeps, Set<String> annotationProcessors
    ) {
        println "generating sub project ${project.name}'s BUCK"
        PrintWriter printWriter = new PrintWriter(
                new FileOutputStream("${project.projectDir.absolutePath}${File.separator}BUCK"))
        printWriter.println("java_library(")
        printWriter.println("\tname = 'src',")
        printWriter.println("\tsrcs = glob(['src/main/java/**/*.java']),")
        printWriter.println("\tdeps = [")
        printWriter.println("\t\t'//.okbuck/${project.name}:all-jars',")
        for (String dep : internalDeps) {
            printWriter.println("\t\t'//${dep}:src',")
        }
        printWriter.println("\t],")
        printWriter.println("\texported_deps = [")
        printWriter.println("\t\t'//.okbuck/${project.name}:all-jars',")
        for (String dep : internalDeps) {
            printWriter.println("\t\t'//${dep}:src',")
        }
        printWriter.println("\t],")
        printWriter.println("\tvisibility = ['PUBLIC'],")

        printWriter.println("\tannotation_processors = [")
        for (String processor : annotationProcessors) {
            printWriter.println("\t\t'${processor}',")
        }
        printWriter.println("\t],")
        printWriter.println("\tannotation_processor_deps = [")
        for (String dep : internalDeps) {
            printWriter.println("\t\t'//${dep}:src',")
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

    private static void copyDependencies(File dir, Set<File> deps) {
        for (File dep : deps) {
            println "copying ${dep.absolutePath} into .okbuck/${dir.name}"
            IOUtils.copy(new FileInputStream(dep), new FileOutputStream(
                    new File(dir.absolutePath + File.separator + dep.name)))
        }
    }

    private static void extractAnnotationProcessors(
            Project project, Map<String, Set<String>> annotationProcessors
    ) {
        Map<String, Set<File>> allSubProjectsAptDeps = new HashMap<>()
        getAllSubProjectsAptDeps(project, allSubProjectsAptDeps, false)
        project.subprojects { prj ->
            annotationProcessors.put(prj.name, new HashSet<String>())

            for (File file : allSubProjectsAptDeps.get(prj.name)) {
                try {
                    JarFile jar = new JarFile(file)
                    for (JarEntry entry : jar.entries()) {
                        if (entry.name.equals(
                                "META-INF/services/javax.annotation.processing.Processor")) {
                            BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(jar.getInputStream(entry)))
                            String processor;
                            while ((processor = reader.readLine()) != null) {
                                annotationProcessors.get(prj.name).add(processor)
                            }
                            reader.close()
                            break
                        }
                    }
                } catch (Exception e) {
                    println "extract processor from ${file.absolutePath} failed!"
                }
            }
        }
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

    private static void genBuckConfig(Project project, File buckConfig) {
        println "generating .buckconfig"
        PrintWriter printWriter = new PrintWriter(new FileOutputStream(buckConfig))
        printWriter.println("[alias]")
        project.subprojects { prj ->
            if (getSubProjectType(prj) == ANDROID_APP_PROJECT) {
                printWriter.println("\t${prj.name} = //${prj.name}:bin")
            } //else
        }

        printWriter.println()
        printWriter.println("[android]")
        printWriter.println("\ttarget = ${project.okbuck.target}")

        printWriter.println()
        printWriter.println("[project]")
        printWriter.println("\tignore = .git")
        printWriter.close()
    }

    /**
     * return values are:
     * unknown: 0
     * Android application project: 1; com.android.build.gradle.AppPlugin was applied;
     * Android library project: 2; com.android.build.gradle.LibraryPlugin was applied;
     * Java library project: 3; org.gradle.api.plugins.JavaPlugin was applied;
     * */
    private static final int UNKNOWN = 0;
    private static final int ANDROID_APP_PROJECT = 1;
    private static final int ANDROID_LIB_PROJECT = 2;
    private static final int JAVA_LIB_PROJECT = 3;

    private static int getSubProjectType(Project project) {
        for (Plugin plugin : project.plugins) {
            if (plugin instanceof AppPlugin) {
                return ANDROID_APP_PROJECT
            } else if (plugin instanceof LibraryPlugin) {
                return ANDROID_LIB_PROJECT
            } else if (plugin instanceof JavaPlugin) {
                return JAVA_LIB_PROJECT
            }
        }

        return UNKNOWN
    }

    private static void printAllSubProjects(Project project) {
        project.subprojects { prj ->
            println "Sub project: ${prj.name}"
        }
    }

    private static void printDeps(
            Project project,
            Map<String, Set<File>> allSubProjectsExternalDeps,
            Map<String, Set<String>> allSubProjectsInternalDeps,
            Map<String, Set<File>> allSubProjectsAptDeps
    ) {
        project.subprojects { prj ->
            println "${prj.name}'s deps:"
            println "<<< internal"
            for (String projectDep : allSubProjectsInternalDeps.get(prj.name)) {
                println "\t${projectDep}"
            }
            println ">>>\n<<< external"
            for (File mavenDep : allSubProjectsExternalDeps.get(prj.name)) {
                println "\t${mavenDep.absolutePath}"
            }
            println ">>>\n<<< apt"
            for (File mavenDep : allSubProjectsAptDeps.get(prj.name)) {
                println "\t${mavenDep.absolutePath}"
            }
            println ">>>"
        }
    }
}

class OkBuckExtension {
    String target = "android-23"
    String keystore = ""
    String keystoreProperties = ""
    boolean overwrite = false
    Map<String, String> resPackages
}
