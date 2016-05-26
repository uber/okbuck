package com.github.okbuilds.core.model

import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.SigningConfig
import com.android.manifmerger.ManifestMerger2
import com.github.okbuilds.core.annotation.Experimental
import com.github.okbuilds.core.dependency.ExternalDependency
import com.github.okbuilds.core.util.FileUtil
import com.github.okbuilds.okbuck.ExperimentalExtension
import com.github.okbuilds.okbuck.OkBuckExtension
import groovy.transform.ToString
import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.tuple.Pair
import org.gradle.api.Project
import org.gradle.api.file.FileTree

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * An Android app target
 */
class AndroidAppTarget extends AndroidLibTarget {

    private static final int DEFAULT_LINEARALLOC_LIMIT = 7194304

    final boolean multidexEnabled
    final boolean debuggable
    final Keystore keystore
    final Set<String> cpuFilters

    final boolean exopackage
    final int linearAllocHardLimit
    final Set<String> primaryDexPatterns
    final Set<String> exoPackageDependencies
    final String appClass

    final boolean minifyEnabled

    @Experimental
    final Map<String, Object> placeholders = [:]

    AndroidAppTarget(Project project, String name) {
        super(project, name)

        multidexEnabled = baseVariant.mergedFlavor.multiDexEnabled
        debuggable = baseVariant.buildType.debuggable
        keystore = extractKeystore()
        if (baseVariant.ndkCompile.abiFilters != null) {
            cpuFilters = baseVariant.ndkCompile.abiFilters
        } else {
            cpuFilters = [] as Set
        }

        OkBuckExtension okbuck = rootProject.okbuck
        exopackage = getProp(okbuck.exopackage, false)
        primaryDexPatterns = getProp(okbuck.primaryDexPatterns, []) as Set
        exoPackageDependencies = getProp(okbuck.appLibDependencies, []) as Set
        linearAllocHardLimit = getProp(okbuck.linearAllocHardLimit, DEFAULT_LINEARALLOC_LIMIT) as Integer
        appClass = extractAppClass()

        minifyEnabled = baseVariant.buildType.minifyEnabled

        ExperimentalExtension experimental = okbuck.experimental
        if (experimental.placeholderSupport) {
            placeholders.put('applicationId', applicationId + applicationIdSuffix)
            placeholders.putAll(baseVariant.buildType.manifestPlaceholders)
            placeholders.putAll(baseVariant.mergedFlavor.manifestPlaceholders)
        }
    }

    @Override
    List<String> getBuildConfigFields() {
        List<String> buildConfig = super.getBuildConfigFields()
        buildConfig.add("String APPLICATION_ID = \"${applicationId + applicationIdSuffix}\"")
        if (versionCode != null) {
            buildConfig.add("int VERSION_CODE = ${versionCode}")
        }
        if (versionName != null) {
            buildConfig.add("String VERSION_NAME = \"${versionName}\"")
        }

        return buildConfig
    }

    @Override
    protected BaseVariant getBaseVariant() {
        return (BaseVariant) project.android.applicationVariants.find { it.name == name }
    }

    @Override
    ManifestMerger2.MergeType getMergeType() {
        return ManifestMerger2.MergeType.APPLICATION
    }

    @Override
    String getManifest() {
        String mergedManifest = super.getManifest()

        XmlSlurper slurper = new XmlSlurper()
        GPathResult manifestXml = slurper.parse(project.file(mergedManifest))

        manifestXml.@package = applicationId + applicationIdSuffix
        manifestXml.@'android:versionCode' = String.valueOf(versionCode)
        manifestXml.@'android:versionName' = versionName
        manifestXml.application.@'android:debuggable' = String.valueOf(debuggable)
        if (manifestXml.'uses-sdk'.size() == 0) {
            manifestXml.appendNode({
                'uses-sdk'('android:minSdkVersion': String.valueOf(minSdk),
                        'android:targetSdkVersion': String.valueOf(targetSdk)) {}
            })
        }

        def builder = new StreamingMarkupBuilder()
        builder.setUseDoubleQuotes(true)
        project.file(mergedManifest).text = (builder.bind {
            mkp.yield manifestXml
        } as String)
                .replaceAll("\\{http://schemas.android.com/apk/res/android\\}versionCode", "android:versionCode")
                .replaceAll("\\{http://schemas.android.com/apk/res/android\\}versionName", "android:versionName")
                .replaceAll("\\{http://schemas.android.com/apk/res/android\\}debuggable", "android:debuggable")
                .replaceAll('xmlns:android="http://schemas.android.com/apk/res/android"', "")
                .replaceAll("<manifest ", '<manifest xmlns:android="http://schemas.android.com/apk/res/android" ')


        return mergedManifest
    }

    Pair<Set<String>, Set<Target>> getAppLibDependencies() {
        Set<String> externalDeps = [] as Set
        Set<Target> projectDeps = [] as Set
        exoPackageDependencies.each { String exoPackageDep ->
            String first // can denote either group or project name
            String last // can denote either module or configuration name
            boolean fullyQualified = false

            if (exoPackageDep.contains(":")) {
                List<String> parts = exoPackageDep.split(":")
                first = parts[0]
                last = parts[1]
                fullyQualified = true
            } else {
                first = last = exoPackageDep
            }

            ExternalDependency external = externalCompileDeps.find { ExternalDependency externalDependency ->
                boolean match = true
                if (fullyQualified) {
                    match &= externalDependency.group == first
                }
                match &= externalDependency.module == last
                return match
            }

            if (external != null) {
                externalDeps.add(dependencyCache.get(external))
            } else {
                Target variantDep = targetCompileDeps.find { Target variant ->
                    boolean match = true
                    if (fullyQualified) {
                        match &= variant.name == last
                    }
                    match &= variant.path == first
                    return match
                }

                if (variantDep != null) {
                    projectDeps.add(variantDep)
                }
            }
        }
        return Pair.of(externalDeps, projectDeps)
    }

    String getProguardConfig() {
        File mergedProguardConfig = project.file("${project.buildDir}/okbuck/${name}/proguard.pro")
        mergedProguardConfig.parentFile.mkdirs()
        Set<File> configs = [] as Set

        // project proguard files
        configs.addAll(baseVariant.mergedFlavor.proguardFiles)
        configs.addAll(baseVariant.buildType.proguardFiles)

        // Consumer proguard files of target dependencies
        configs.addAll((targetCompileDeps.findAll { Target target ->
            target instanceof AndroidLibTarget
        } as List<AndroidLibTarget>).collect { AndroidLibTarget target ->
            target.baseVariant.mergedFlavor.consumerProguardFiles +
                    target.baseVariant.buildType.consumerProguardFiles
        }.flatten() as Set<File>)

        String mergedConfig = ""
        configs.each { File config ->
            mergedConfig += "\n##---- ${config} ----##\n"
            mergedConfig += config.text
        }

        // Consumer proguard files of compiled aar dependencies
        compileDeps.findAll { String dep ->
            dep.endsWith(".aar")
        }.each { String dep ->
            String config = getPackedProguardConfig(rootProject.file(dep))
            if (!config.empty) {
                mergedConfig += "\n##---- ${FilenameUtils.getBaseName(dep)} ----##\n"
                mergedConfig += config
            }
        }

        mergedProguardConfig.text = mergedConfig
        return FileUtil.getRelativePath(project.projectDir, mergedProguardConfig)
    }

    private static String getPackedProguardConfig(File file) {
        ZipFile zipFile = new ZipFile(file)
        ZipEntry proguardEntry = zipFile.entries().find {
            !it.directory && it.name == "proguard.txt"
        } as ZipEntry
        if (proguardEntry != null) {
            return zipFile.getInputStream(proguardEntry).text
        } else {
            return ''
        }
    }

    private Keystore extractKeystore() {
        SigningConfig config = baseVariant.mergedFlavor.signingConfig
        if (config == null) {
            config = baseVariant.buildType.signingConfig
        }

        if (config != null) {
            return new Keystore(config.storeFile, config.keyAlias, config.storePassword, config.keyPassword)
        } else {
            return null
        }
    }

    private String extractAppClass() {
        String appClass = null
        XmlSlurper slurper = new XmlSlurper()
        slurper.DTDHandler = null
        GPathResult manifestXml = slurper.parse(project.file(manifest))
        try {
            appClass = manifestXml.application.@"android:name"
            appClass = appClass.replaceAll('\\.', "/")
        } catch (Exception ignored) {
        }
        if (appClass != null && !appClass.empty) {
            sources.each { String sourceDir ->
                FileTree found = project.fileTree(dir: sourceDir, includes:
                        ["${appClass}.java"])
                if (found.size() == 1) {
                    appClass = FileUtil.getRelativePath(project.projectDir, found[0])
                }
            }
        }
        return appClass
    }

    @ToString(includes = [])
    static class Keystore {

        final File storeFile
        final String alias
        final String storePassword
        final String keyPassword

        Keystore(File storeFile, String alias, String storePassword, String keyPassword) {
            this.storeFile = storeFile
            this.alias = alias
            this.storePassword = storePassword
            this.keyPassword = keyPassword
        }
    }
}
