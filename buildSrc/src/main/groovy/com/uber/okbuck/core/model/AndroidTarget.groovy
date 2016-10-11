package com.uber.okbuck.core.model

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import com.android.build.gradle.internal.api.TestedVariant
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.builder.model.ClassField
import com.android.builder.model.SourceProvider
import com.android.manifmerger.ManifestMerger2
import com.android.manifmerger.MergingReport
import com.android.utils.ILogger
import com.uber.okbuck.core.util.FileUtil
import groovy.transform.ToString
import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.compile.JavaCompile

/**
 * An Android target
 */
abstract class AndroidTarget extends JavaLibTarget {

    static final String DEFAULT_RES_NAME = "main"

    final String applicationId
    final String applicationIdSuffix
    final String versionName
    final Integer versionCode
    final int minSdk
    final int targetSdk
    final boolean debuggable
    final boolean generateR2

    String manifestPath
    String packageName

    AndroidTarget(Project project, String name) {
        super(project, name)

        String suffix = ""
        if (baseVariant.mergedFlavor.applicationIdSuffix != null) {
            suffix += baseVariant.mergedFlavor.applicationIdSuffix
        }
        if (baseVariant.buildType.applicationIdSuffix != null) {
            suffix += baseVariant.buildType.applicationIdSuffix
        }

        applicationIdSuffix = suffix
        applicationId = baseVariant.applicationId - applicationIdSuffix
        versionName = baseVariant.mergedFlavor.versionName
        versionCode = baseVariant.mergedFlavor.versionCode

        debuggable = baseVariant.buildType.debuggable

        // Butterknife support
        generateR2 = project.plugins.hasPlugin('com.jakewharton.butterknife')

        if (baseVariant.mergedFlavor.minSdkVersion == null ||
                baseVariant.mergedFlavor.targetSdkVersion == null) {
            minSdk = 1
            targetSdk = 1
            throw new IllegalStateException("module `" + project.name +
                    "` must specify minSdkVersion and targetSdkVersion in build.gradle")
        } else {
            minSdk = baseVariant.mergedFlavor.minSdkVersion.apiLevel
            targetSdk = baseVariant.mergedFlavor.targetSdkVersion.apiLevel
        }
    }

    protected abstract BaseVariant getBaseVariant()

    protected abstract ManifestMerger2.MergeType getMergeType()

    @Override
    Scope getMain() {
        Set<File> srcDirs = baseVariant.sourceSets.collect { SourceProvider provider ->
            provider.javaDirectories
        }.flatten() as Set<File>

        return new Scope(
                project,
                ["compile", "${buildType}Compile", "${flavor}Compile", "${name}Compile"] as Set,
                srcDirs,
                null,
                getJavaCompilerOptions(baseVariant) + extraJvmArgs)
    }

    @Override
    Scope getTest() {
        return new Scope(
                project,
                ["compile", "${buildType}Compile", "${flavor}Compile", "${name}Compile",
                 "testCompile", "${buildType}TestCompile", "${flavor}TestCompile", "${name}TestCompile"] as Set,
                project.files("src/test/java") as Set<File>,
                project.file("src/test/resources"),
                getJavaCompilerOptions(unitTestVariant) + extraJvmArgs)
    }

    @Override
    Set<GradleSourceGen> getGradleSourcegen() {
        Set<GradleSourceGen> tasks = super.getGradleSourcegen()
        // SqlDelight support
        if (project.plugins.hasPlugin('com.squareup.sqldelight')) {
            BaseVariantData data = baseVariant.variantData
            Task sqlDelightGen = data.sourceGenTask.getDependsOn().find {
                it instanceof Task && it.name.toLowerCase().contains("sqldelight")
            } as Task
            if (sqlDelightGen) {
                tasks.add(new GradleSourceGen(sqlDelightGen,
                        srcDirNames.collect { "src/${it}/sqldelight/**/*.sq" },
                        sqlDelightGen.outputs.files[0]))
            }
        }
        return tasks
    }

    Set<String> getSrcDirNames() {
        return baseVariant.sourceSets.collect { SourceProvider provider ->
            provider.name
        }
    }

    public boolean getRobolectric() {
        return rootProject.okbuck.test.robolectric
    }

    @Override
    String getSourceCompatibility() {
        return javaVersion(project.android.compileOptions.sourceCompatibility as JavaVersion)
    }

    @Override
    String getTargetCompatibility() {
        return javaVersion(project.android.compileOptions.targetCompatibility as JavaVersion)
    }

    @SuppressWarnings("Deprecated")
    @Override
    String getInitialBootCp() {
        return baseVariant.javaCompile.options.bootClasspath
    }

    List<String> getBuildConfigFields() {
        List<String> buildConfig = [
                "String APPLICATION_ID = \"${applicationId + applicationIdSuffix}\"",
                "String BUILD_TYPE = \"${buildType}\"",
                "String FLAVOR = \"${flavor}\"",
        ]
        if (versionCode != null) {
            buildConfig.add("int VERSION_CODE = ${versionCode}")
        }
        if (versionName != null) {
            buildConfig.add("String VERSION_NAME = \"${versionName}\"")
        }

        Map<String, ClassField> extraBuildConfig = [:]

        baseVariant.buildType.buildConfigFields.collect { String key, ClassField classField ->
            extraBuildConfig.put(key, classField)
        }

        baseVariant.mergedFlavor.buildConfigFields.collect { String key, ClassField classField ->
            extraBuildConfig.put(key, classField)
        }

        buildConfig += extraBuildConfig.collect {
            String key, ClassField classField ->
                "${classField.type} ${key} = ${classField.value}"
        }

        return buildConfig
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

        Map<File, String> resourceMap = resources.collectEntries { String res ->
            [project.file(res).parentFile, res]
        }
        Map<File, String> assetMap = assets.collectEntries { String asset ->
            [project.file(asset).parentFile, asset]
        }

        Set<File> keys = (resourceMap.keySet() + assetMap.keySet())
        Set<ResBundle> resBundles = keys.collect { key ->
            new ResBundle(key.name, resourceMap.get(key, null), assetMap.get(key, null))
        } as Set

        // Add an empty resource bundle even if no res and assets folders exist since we use resource_union
        if (resBundles.empty) {
            resBundles.add(new ResBundle(DEFAULT_RES_NAME, null, null))
        }
        return resBundles
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

    String getPackage() {
        if (!packageName) {
            ensureManifest()
        }
        return packageName
    }

    String getManifest() {
        if (!manifestPath) {
            ensureManifest()
        }
        return manifestPath
    }

    private void ensureManifest() {
        Set<String> manifests = [] as Set

        baseVariant.sourceSets.each { SourceProvider provider ->
            manifests.addAll(getAvailable(Collections.singletonList(provider.manifestFile)))
        }

        if (manifests.empty) {
            return
        }

        File mainManifest = project.file(manifests[0])

        List<File> secondaryManifests = []
        secondaryManifests.addAll(
                manifests.collect {
                    String manifestFile -> project.file(manifestFile)
                })

        secondaryManifests.remove(mainManifest)

        File mergedManifest = project.file("${project.buildDir}/okbuck/${name}/AndroidManifest.xml")
        mergedManifest.parentFile.mkdirs()
        mergedManifest.createNewFile()

        MergingReport report = ManifestMerger2.newMerger(mainManifest, new GradleLogger(project.logger), mergeType)
                .addFlavorAndBuildTypeManifests(secondaryManifests as File[])
                .withFeatures(ManifestMerger2.Invoker.Feature.NO_PLACEHOLDER_REPLACEMENT) // needs to be handled by buck
                .merge()

        if (report.result.success) {
            mergedManifest.text = report.getMergedDocument(MergingReport.MergedManifestKind.MERGED)

            XmlSlurper slurper = new XmlSlurper()
            GPathResult manifestXml = slurper.parse(project.file(mergedManifest))

            packageName = manifestXml.@package
            manifestXml.@package = applicationId + applicationIdSuffix

            if (versionCode) {
                manifestXml.@'android:versionCode' = String.valueOf(versionCode)
            }
            if (versionName) {
                manifestXml.@'android:versionName' = versionName
            }
            manifestXml.application.@'android:debuggable' = String.valueOf(debuggable)

            def sdkNode = {
                'uses-sdk'('android:minSdkVersion': String.valueOf(minSdk),
                        'android:targetSdkVersion': String.valueOf(targetSdk)) {}
            }
            if (manifestXml.'uses-sdk'.size() == 0) {
                manifestXml.appendNode(sdkNode)
            } else {
                manifestXml.'uses-sdk'.replaceNode(sdkNode)
            }

            def builder = new StreamingMarkupBuilder()
            builder.setUseDoubleQuotes(true)
            mergedManifest.text = (builder.bind {
                mkp.yield manifestXml
            } as String)
                    .replaceAll("\\{http://schemas.android.com/apk/res/android\\}versionCode", "android:versionCode")
                    .replaceAll("\\{http://schemas.android.com/apk/res/android\\}versionName", "android:versionName")
                    .replaceAll("\\{http://schemas.android.com/apk/res/android\\}debuggable", "android:debuggable")
                    .replaceAll('xmlns:android="http://schemas.android.com/apk/res/android"', "")
                    .replaceAll("<manifest ", '<manifest xmlns:android="http://schemas.android.com/apk/res/android" ')
        } else if (report.result.error) {
            throw new IllegalStateException(report.loggingRecords.collect {
                "${it.severity}: ${it.message} at ${it.sourceLocation}"
            }.join('\n'))
        }

        manifest = FileUtil.getRelativePath(project.projectDir, mergedManifest)
    }

    static List<String> getJavaCompilerOptions(BaseVariant baseVariant) {
        if (baseVariant != null && baseVariant.javaCompiler instanceof JavaCompile) {
            List<String> options = ((JavaCompile) baseVariant.javaCompiler).options.compilerArgs
            // Remove options added by apt plugin since they are handled by apt scope separately
            filterOptions(options, ["-s", "-processorpath"])
            return options
        } else {
            return []
        }
    }

    static void filterOptions(List<String> options, List<String> remove) {
        remove.each { String key ->
            int index = options.indexOf(key)
            if (index != -1) {
                options.remove(index + 1)
                options.remove(index)
            }
        }
    }

    UnitTestVariant getUnitTestVariant() {
        if (baseVariant instanceof TestedVariant) {
            return ((TestedVariant) baseVariant).unitTestVariant
        } else {
            return null
        }
    }

    TestVariant getInstrumentationTestVariant() {
        if (baseVariant instanceof TestedVariant) {
            TestVariant testVariant = ((TestedVariant) baseVariant).testVariant
            if (testVariant != null) {
                Set<String> manifests = [] as Set
                testVariant.sourceSets.each { SourceProvider provider ->
                    manifests.addAll(getAvailable(Collections.singletonList(provider.manifestFile)))
                }
                return manifests.empty ? null : testVariant
            } else {
                return null
            }
        } else {
            return null
        }
    }

    @ToString(includeNames = true)
    static class ResBundle {

        String name
        String resDir
        String assetsDir

        ResBundle(String name, String resDir, String assetsDir) {
            this.name = name
            this.resDir = resDir
            this.assetsDir = assetsDir
        }
    }

    @Override
    def getProp(Map map, defaultValue) {
        return map.get("${identifier}${name.capitalize()}" as String,
                map.get("${identifier}${flavor.capitalize()}" as String,
                        map.get("${identifier}${buildType.capitalize()}" as String,
                                map.get(identifier, defaultValue))))
    }

    private static class GradleLogger implements ILogger {

        private final Logger mLogger;

        GradleLogger(Logger logger) {
            mLogger = logger;
        }

        @Override
        void error(Throwable throwable, String s, Object... objects) {
            mLogger.error(s, objects)
        }

        @Override
        void warning(String s, Object... objects) {
            mLogger.warn(s, objects)
        }

        @Override
        void info(String s, Object... objects) {
            mLogger.info(s, objects)
        }

        @Override
        void verbose(String s, Object... objects) {
            mLogger.debug(s, objects)
        }
    }

}
