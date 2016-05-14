package com.github.piasy.okbuck.model

import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.SigningConfig
import com.github.piasy.okbuck.OkBuckExtension
import com.github.piasy.okbuck.dependency.ExternalDependency
import com.github.piasy.okbuck.util.FileUtil
import groovy.transform.ToString
import groovy.util.slurpersupport.GPathResult
import org.apache.commons.lang3.tuple.Pair
import org.gradle.api.Project
import org.gradle.api.file.FileTree

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
    }

    @Override
    List<String> getBuildConfigFields() {
        List<String> buildConfig = super.getBuildConfigFields()
        buildConfig.add("String APPLICATION_ID = \"${applicationId}\"")
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
    protected void manipulateManifest(GPathResult manifest) {
        manifest.@'android:versionCode' = versionCode.toString()
        manifest.@'android:versionName' = versionName

        if (manifest.'uses-sdk'.size() == 0) {
            manifest.appendNode({
                'uses-sdk'('android:minSdkVersion': new Integer(minSdk).toString(),
                        'android:targetSdkVersion': new Integer(targetSdk).toString()) {}
            })
        } else {
            manifest.'uses-sdk'.@'android:minSdkVersion' = new Integer(minSdk).toString()
            manifest.'uses-sdk'.@'android:targetSdkVersion' = new Integer(targetSdk).toString()
        }
    }

    Pair<Set<String>, Set<Target>> getAppLibDependencies() {
        Set<String> externalDeps = [] as Set
        Set<Target> projectDeps = [] as Set
        exoPackageDependencies.each { String exoPackageDep ->
            String first // can denote either group or project name
            String last // can denote either module or configuration name
            boolean fullyQualified = false

            if (exoPackageDep.contains(";")) {
                List<String> parts = exoPackageDep.split(";")
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

    private Keystore extractKeystore() {
        SigningConfig config = baseVariant.mergedFlavor.signingConfig
        if (config == null) {
            config = baseVariant.buildType.signingConfig
        }

        if (config != null) {
            return new Keystore(config.storeFile, config.keyAlias, config.keyPassword, config.storePassword)
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
