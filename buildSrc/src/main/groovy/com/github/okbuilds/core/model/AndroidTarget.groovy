package com.github.okbuilds.core.model

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.tasks.InvokeManifestMerger
import com.android.builder.model.ClassField
import com.android.builder.model.SourceProvider
import com.github.okbuilds.core.dependency.DependencyCache
import com.github.okbuilds.core.util.FileUtil
import groovy.transform.ToString
import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException

/**
 * An Android target
 */
abstract class AndroidTarget extends JavaLibTarget {

    final String applicationId
    final String applicationIdWithSuffix
    final String versionName
    final Integer versionCode
    final int minSdk
    final int targetSdk
    final String manifest

    AndroidTarget(Project project, String name) {
        super(project, name)

        applicationId = baseVariant.applicationId
        String suffix = ""
        if (baseVariant.mergedFlavor.applicationIdSuffix != null) {
            suffix += baseVariant.mergedFlavor.applicationIdSuffix
        }
        applicationIdWithSuffix = applicationId + suffix
        versionName = baseVariant.mergedFlavor.versionName
        versionCode = baseVariant.mergedFlavor.versionCode
        minSdk = baseVariant.mergedFlavor.minSdkVersion.apiLevel
        targetSdk = baseVariant.mergedFlavor.targetSdkVersion.apiLevel
        manifest = extractMergedManifest()
    }

    protected abstract BaseVariant getBaseVariant()

    @Override
    protected Set<File> sourceDirs() {
        return baseVariant.sourceSets.collect { SourceProvider provider ->
            provider.javaDirectories
        }.flatten() as Set<File>
    }

    @Override
    protected Set<String> compileConfigurations() {
        return ["compile", "${buildType}Compile", "${flavor}Compile", "${name}Compile"]
    }

    @Override
    String getSourceCompatibility() {
        return javaVersion(project.android.compileOptions.sourceCompatibility)
    }

    @Override
    String getTargetCompatibility() {
        return javaVersion(project.android.compileOptions.targetCompatibility)
    }

    @Override
    List<String> getJvmArgs() {
        return extraJvmArgs
    }

    @Override
    String getInitialBootCp() {
        return baseVariant.javaCompile.options.bootClasspath
    }

    List<String> getBuildConfigFields() {
        return ["String BUILD_TYPE = \"${buildType}\"",
                "String FLAVOR = \"${flavor}\"",
        ].plus(baseVariant.mergedFlavor.buildConfigFields.collect {
            String key, ClassField classField ->
                "${classField.type} ${key} = ${classField.value}"
        })
    }

    String getFlavor() {
        return baseVariant.flavorName
    }

    String getBuildType() {
        return baseVariant.buildType.name
    }

    Set<ResBundle> getResources() {
        Set<String> resources = [] as Set
        Set<String> assets = [] as Set

        baseVariant.sourceSets.each { SourceProvider provider ->
            resources.addAll(getAvailable(provider.resDirectories))
            assets.addAll(getAvailable(provider.assetsDirectories))
        }

        Map<String, String> resourceMap = resources.collectEntries { String res ->
            [project.file(res).parentFile.path, res]
        }
        Map<String, String> assetMap = assets.collectEntries { String asset ->
            [project.file(asset).parentFile.path, asset]
        }

        return resourceMap.keySet().plus(assetMap.keySet()).collect { key ->
            new ResBundle(identifier, resourceMap.get(key, null), assetMap.get(key, null))
        } as Set
    }

    Set<String> getAidl() {
        baseVariant.sourceSets.collect { SourceProvider provider ->
            getAvailable(provider.aidlDirectories)
        }.flatten() as Set<String>
    }

    Set<String> getJniLibs() {
        baseVariant.sourceSets.collect { SourceProvider provider ->
            getAvailable(provider.jniLibsDirectories)
        }.flatten() as Set<String>
    }

    protected String extractMergedManifest() {
        Set<String> manifests = [] as Set

        baseVariant.sourceSets.each { SourceProvider provider ->
            manifests.addAll(getAvailable(Collections.singletonList(provider.manifestFile)))
        }

        if (manifests.empty) {
            return null
        }

        File mainManifest = project.file(manifests[manifests.size() - 1])

        List<File> secondaryManifests = []
        secondaryManifests.addAll(manifests.collect {
            String manifestFile -> project.file(manifestFile)
        })
        secondaryManifests.remove(mainManifest)

        File mergedManifest = project.file("${project.buildDir}/okbuck/${name}/AndroidManifest.xml")
        mergedManifest.parentFile.mkdirs()
        mergedManifest.createNewFile()
        mergedManifest.text = ""

        String manifestMergeTaskname = "okbuckMerge${name}Manifest"
        InvokeManifestMerger manifestMerger
        try {
            manifestMerger = (InvokeManifestMerger) project.tasks.getByName(manifestMergeTaskname)
        } catch (UnknownTaskException ignored) {
            manifestMerger = project.tasks.create("okbuckMerge${name}Manifest",
                    InvokeManifestMerger, {
                it.mainManifestFile = mainManifest;
                it.secondaryManifestFiles = secondaryManifests;
                it.outputFile = mergedManifest
            })
        }

        manifestMerger.doFullTaskAction()

        XmlSlurper slurper = new XmlSlurper()
        GPathResult manifestXml = slurper.parse(mergedManifest)

        manipulateManifest(manifestXml)

        def builder = new StreamingMarkupBuilder()
        builder.setUseDoubleQuotes(true)
        mergedManifest.text = builder.bind {
            mkp.yield manifestXml
        } as String

        mergedManifest.text = mergedManifest.text
                .replaceAll("\\{http://schemas.android.com/apk/res/android\\}versionCode", "android:versionCode")
                .replaceAll("\\{http://schemas.android.com/apk/res/android\\}versionName", "android:versionName")
                .replaceAll('xmlns:android="http://schemas.android.com/apk/res/android"', "")
                .replaceAll("<manifest ", '<manifest xmlns:android="http://schemas.android.com/apk/res/android" ')

        return FileUtil.getRelativePath(project.projectDir, mergedManifest)
    }

    protected void manipulateManifest(GPathResult manifest) {}

    @ToString(includeNames = true)
    static class ResBundle {

        String id
        String resDir
        String assetsDir

        ResBundle(String identifier, String resDir, String assetsDir) {
            this.resDir = resDir
            this.assetsDir = assetsDir
            id = DependencyCache.md5("${identifier}:${resDir}:${assetsDir}")
        }
    }

    @Override
    def getProp(Map map, defaultValue) {
        return map.get("${identifier}${name.capitalize()}" as String,
                map.get("${identifier}${flavor.capitalize()}" as String,
                        map.get("${identifier}${buildType.capitalize()}" as String,
                                map.get(identifier, defaultValue))))
    }
}
