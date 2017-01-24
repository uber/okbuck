package com.uber.okbuck.core.model.android

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import com.android.build.gradle.internal.api.TestedVariant
import com.android.builder.core.VariantType
import com.android.builder.model.ClassField
import com.android.builder.model.LintOptions
import com.android.builder.model.SourceProvider
import com.android.manifmerger.ManifestMerger2
import com.android.manifmerger.MergingReport
import com.android.utils.ILogger
import com.uber.okbuck.core.model.base.Scope
import com.uber.okbuck.core.model.java.JavaLibTarget
import com.uber.okbuck.core.util.FileUtil
import groovy.util.slurpersupport.GPathResult
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

/**
 * An Android target
 */
abstract class AndroidTarget extends JavaLibTarget {

    static final String DEFAULT_RES_NAME = "main"
    static final EmptyLogger EMPTY_LOGGER = new EmptyLogger()

    final String applicationId
    final String applicationIdSuffix
    final String versionName
    final Integer versionCode
    final int minSdk
    final int targetSdk
    final boolean debuggable
    final boolean generateR2

    private String manifestPath
    private String packageName
    boolean isTest

    AndroidTarget(Project project, String name, boolean isTest = false) {
        super(project, name)
        this.isTest = isTest

        String suffix = ""
        if (baseVariant.mergedFlavor.applicationIdSuffix != null) {
            suffix += baseVariant.mergedFlavor.applicationIdSuffix
        }
        if (baseVariant.buildType.applicationIdSuffix != null) {
            suffix += baseVariant.buildType.applicationIdSuffix
        }

        applicationIdSuffix = suffix
        if (isTest) {
            String applicationIdString = baseVariant.applicationId - ".test" - applicationIdSuffix
            applicationId = applicationIdString - applicationIdSuffix
        } else {
            applicationId = baseVariant.applicationId - applicationIdSuffix
        }
        versionName = baseVariant.mergedFlavor.versionName
        versionCode = baseVariant.mergedFlavor.versionCode

        debuggable = baseVariant.buildType.debuggable

        // Butterknife support
        generateR2 = project.plugins.hasPlugin('com.jakewharton.butterknife')

        if (baseVariant.mergedFlavor.minSdkVersion == null ||
                baseVariant.mergedFlavor.targetSdkVersion == null) {
            minSdk = targetSdk = 1
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
                getJavaCompilerOptions(baseVariant))
    }

    @Override
    Scope getTest() {
        return new Scope(
                project,
                ["compile", "${buildType}Compile", "${flavor}Compile", "${name}Compile",
                 "testCompile", "${buildType}TestCompile", "${flavor}TestCompile", "${name}TestCompile"] as Set,
                project.files("src/test/java") as Set<File>,
                project.file("src/test/resources"),
                getJavaCompilerOptions(unitTestVariant))
    }

    @Override
    Set<String> getDepConfigNames() {
        return super.getDepConfigNames() +
                ["compile", "${buildType}Compile", "${flavor}Compile", "${name}Compile",
                 "testCompile", "${buildType}TestCompile", "${flavor}TestCompile",
                 "${name}TestCompile",
                 "androidTestApt",
                 "androidTestCompile",
                 "androidTest${buildType.capitalize()}Compile",
                 "androidTest${flavor.capitalize()}Compile",
                 "androidTest${name}Compile"]
    }

    @Override
    LintOptions getLintOptions() {
        return project.android.lintOptions
    }

    boolean getRobolectric() {
        return okbuck.test.robolectric
    }

    @Override
    String getSourceCompatibility() {
        return javaVersion(project.android.compileOptions.sourceCompatibility as JavaVersion)
    }

    @Override
    String getTargetCompatibility() {
        return javaVersion(project.android.compileOptions.targetCompatibility as JavaVersion)
    }

    @Override
    List<String> getTestRunnerJvmArgs() {
        Test testTask = project.tasks.withType(Test).find {
            it.name == "${VariantType.UNIT_TEST.prefix}${name.capitalize()}${VariantType.UNIT_TEST.suffix}" as String
        }
        return testTask != null ? testTask.allJvmArgs : []
    }

    List<String> getBuildConfigFields() {
        List<String> buildConfig = [
                isTest ? "String APPLICATION_ID = \"${applicationId + applicationIdSuffix + ".test"}\""
                        : "String APPLICATION_ID = \"${applicationId + applicationIdSuffix}\"",
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
            new ResBundle(key.name, resourceMap.get(key), assetMap.get(key))
        } as Set<ResBundle>

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

    abstract String processManifestXml(GPathResult manifestXml)

    private void ensureManifest() {
        Set<String> manifests = getAvailable(baseVariant.sourceSets.collect { SourceProvider provider ->
            provider.manifestFile
        })

        // Nothing to merge
        if (manifests.empty) {
            return
        }

        File mergedManifest = project.file("${project.buildDir}/okbuck/${name}/AndroidManifest.xml")
        mergedManifest.parentFile.mkdirs()
        mergedManifest.createNewFile()

        if (manifests.size() == 1) { // No need to merge
            parseManifest(project.file(manifests[0]).text, mergedManifest)
        } else {
            File mainManifest = project.file(manifests[0])
            List<File> secondaryManifests = []
            secondaryManifests.addAll(
                    manifests.collect {
                        String manifestFile -> project.file(manifestFile)
                    })

            secondaryManifests.remove(mainManifest)

            MergingReport report =
                    ManifestMerger2.newMerger(mainManifest, EMPTY_LOGGER, mergeType) // errors are reported later
                            .addFlavorAndBuildTypeManifests(secondaryManifests as File[])
                            .withFeatures(ManifestMerger2.Invoker.Feature.NO_PLACEHOLDER_REPLACEMENT) // handled by buck
                            .merge()

            if (report.result.success) {
                parseManifest(report.getMergedDocument(MergingReport.MergedManifestKind.MERGED), mergedManifest)
            } else {
                throw new IllegalStateException(report.loggingRecords.collect {
                    "${it.severity}: ${it.message} at ${it.sourceLocation}"
                }.join('\n'))
            }
        }
        manifestPath = FileUtil.getRelativePath(project.projectDir, mergedManifest)
    }

    private void parseManifest(String originalManifest, File mergedManifest) {
        XmlSlurper slurper = new XmlSlurper()
        GPathResult manifestXml = slurper.parseText(originalManifest)
        packageName = manifestXml.@package

        String processedManifest = processManifestXml(manifestXml)
        if (processedManifest) {
            mergedManifest.text = processedManifest
        } else {
            mergedManifest.text = originalManifest
        }
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
        String nameKey = "${identifier}${name.capitalize()}" as String
        String flavorKey = "${identifier}${flavor.capitalize()}" as String
        String buildTypeKey = "${identifier}${buildType.capitalize()}" as String

        if (map.containsKey(nameKey)) {
            return map.get(nameKey)
        } else if (map.containsKey(flavorKey)) {
            return map.get(flavorKey)
        } else if (map.containsKey(buildTypeKey)) {
            return map.get(buildTypeKey)
        } else if (map.containsKey(identifier)) {
            return map.get(identifier)
        } else {
            return defaultValue
        }
    }

    private static class EmptyLogger implements ILogger {

        @Override
        void error(Throwable throwable, String s, Object... objects) {
            // ignore
        }

        @Override
        void warning(String s, Object... objects) {
            // ignore
        }

        @Override
        void info(String s, Object... objects) {
            // ignore
        }

        @Override
        void verbose(String s, Object... objects) {
            //ignore
        }
    }
}
