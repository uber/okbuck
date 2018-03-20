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
import com.google.common.collect.ImmutableSet
import com.uber.okbuck.OkBuckGradlePlugin
import com.uber.okbuck.core.model.base.RuleType
import com.uber.okbuck.core.model.base.Scope
import com.uber.okbuck.core.model.java.JavaLibTarget
import com.uber.okbuck.core.model.jvm.TestOptions
import com.uber.okbuck.core.model.kotlin.KotlinLibTarget
import com.uber.okbuck.core.util.FileUtil
import groovy.util.slurpersupport.GPathResult
import groovy.xml.StreamingMarkupBuilder
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper

import java.nio.file.Paths

import static com.uber.okbuck.core.util.KotlinUtil.KOTLIN_ANDROID_EXTENSIONS_MODULE

/**
 * An Android target
 */
abstract class AndroidTarget extends JavaLibTarget {

    private static final EmptyLogger EMPTY_LOGGER = new EmptyLogger()

    final String applicationId
    final String applicationIdSuffix
    final String versionName
    final Integer versionCode
    final int minSdk
    final int targetSdk
    final boolean debuggable
    final boolean generateR2
    final String genDir
    final boolean isKotlin
    final boolean isKapt
    final boolean hasKotlinAndroidExtensions
    final boolean hasExperimentalKotlinAndroidExtensions
    final boolean lintExclude
    final boolean testExclude

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

        // Create gen dir
        genDir = Paths.get(OkBuckGradlePlugin.OKBUCK_GEN, path, name).toString()
        FileUtil.copyResourceToProject("gen/BUCK_FILE",
                rootProject.file(genDir).toPath().resolve(OkBuckGradlePlugin.BUCK).toFile())

        // Check if kotlin
        isKotlin = project.plugins.hasPlugin(KotlinAndroidPluginWrapper.class)
        isKapt = project.plugins.hasPlugin(Kapt3GradleSubplugin.class)
        hasKotlinAndroidExtensions = project.plugins.hasPlugin(KOTLIN_ANDROID_EXTENSIONS_MODULE)

        // Check if any rules are excluded
        lintExclude = ((Set) getProp(okbuck.lintExclude, ImmutableSet.of())).contains(name)
        testExclude = ((Set) getProp(okbuck.testExclude, ImmutableSet.of())).contains(name)

        try {
            hasExperimentalKotlinAndroidExtensions = hasKotlinAndroidExtensions &&
                                                     project.androidExtensions.experimental
        } catch (Exception ignored) {
            hasExperimentalKotlinAndroidExtensions = false
        }

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
        return Scope.from(
                project,
                baseVariant.runtimeConfiguration,
                getSources(baseVariant),
                null,
                getJavaCompilerOptions(baseVariant))
    }

    @Override
    Scope getTest() {
        return Scope.from(
                project,
                unitTestVariant ? unitTestVariant.runtimeConfiguration : null,
                unitTestVariant ? getSources(unitTestVariant): ImmutableSet.of(),
                project.file("src/test/resources"),
                getJavaCompilerOptions(unitTestVariant))
    }

    @Override
    Scope getApt() {
        if (isKapt) {
            return Scope.from(project, "kapt${baseVariant.name.capitalize()}")
        }
        return Scope.from(project, baseVariant.annotationProcessorConfiguration)
    }

    @Override
    Scope getTestApt() {
        if (isKapt)  {
            return Scope.from(project, "kapt${baseVariant.getName().capitalize()}")
        }
        return Scope.from(project, unitTestVariant ? unitTestVariant.annotationProcessorConfiguration : null)
    }

    @Override
    Scope getProvided() {
        return Scope.from(project, baseVariant.compileConfiguration)
    }

    @Override
    Scope getTestProvided() {
        return Scope.from(project, unitTestVariant
                ? unitTestVariant.compileConfiguration : null
        )
    }

    @Override
    LintOptions getLintOptions() {
        return project.android.lintOptions
    }

    boolean getRobolectricEnabled() {
        return okbuck.test.robolectric && !testExclude
    }

    boolean getLintEnabled() {
        return !okbuck.lint.disabled && !lintExclude
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
    TestOptions getTestOptions() {
        Test testTask = project.tasks.withType(Test).find {
            it.name == "${VariantType.UNIT_TEST.prefix}${name.capitalize()}${VariantType.UNIT_TEST.suffix}" as String
        }
        List<String> jvmArgs = testTask != null ? testTask.getAllJvmArgs() : Collections.<String>emptyList()
        Map<String, Object> env = testTask != null ? testTask.getEnvironment() : Collections.emptyMap()
        env.keySet().removeAll(System.getenv().keySet())
        return new TestOptions(jvmArgs, env)
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

        baseVariant.mergedFlavor.buildConfigFields.collect { String key, ClassField classField ->
            extraBuildConfig.put(key, classField)
        }

        baseVariant.buildType.buildConfigFields.collect { String key, ClassField classField ->
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

    Set<String> getResDirs() {
        return getAvailable(baseVariant.sourceSets.collect { SourceProvider provider ->
            provider.resDirectories
        }.flatten() as Set<String>)
    }

    /**
     * Returns a map of each resource directory to its corresponding variant
     */
    Map<String, String> getResVariantDirs() {
        Map<String, String> variantDirs = new HashMap<>()
        for (SourceProvider provider : baseVariant.sourceSets) {
            for (String dir : getAvailable(provider.resDirectories)) {
                variantDirs.put(dir, provider.name)
            }
        }
        return variantDirs
    }

    Set<String> getAssetDirs() {
        return getAvailable(baseVariant.sourceSets.collect { SourceProvider provider ->
            provider.assetsDirectories
        }.flatten() as Set<String>)
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

    String processManifestXml(GPathResult manifestXml) {
        def sdkNode = getSdkNode(manifestXml, minSdk, targetSdk)
        if (manifestXml.'uses-sdk'.size() == 0) {
            manifestXml.appendNode(sdkNode)
        } else {
            manifestXml.'uses-sdk'.replaceNode(sdkNode)
        }

        def builder = new StreamingMarkupBuilder()
        builder.setUseDoubleQuotes(true)
        return (builder.bind {
            mkp.yield manifestXml
        } as String)
            .replaceAll('xmlns:android="http://schemas.android.com/apk/res/android"', '')
            .replaceFirst('<manifest ', '<manifest xmlns:android="http://schemas.android.com/apk/res/android" ')
    }

    private void ensureManifest() {
        Set<String> manifests = getAvailable(baseVariant.sourceSets.collect { SourceProvider provider ->
            provider.manifestFile
        })

        // Nothing to merge
        if (manifests.empty) {
            return
        }

        File mergedManifest = getGenPath("AndroidManifest.xml")
        mergedManifest.parentFile.mkdirs()
        mergedManifest.createNewFile()

        if (manifests.size() == 1 && mergeType == ManifestMerger2.MergeType.LIBRARY) { // No need to merge for libraries
            parseManifest(project.file(manifests[0]).text, mergedManifest)
        } else { // always merge if more than one manifest or its an application
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
        manifestPath = FileUtil.getRelativePath(project.rootDir, mergedManifest)
    }

    private void parseManifest(String originalManifest, File mergedManifest) {
        XmlSlurper slurper = new XmlSlurper(false, false)
        GPathResult manifestXml = slurper.parseText(originalManifest)
        packageName = manifestXml.@package

        String processedManifest = processManifestXml(manifestXml)
        mergedManifest.text = processedManifest
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

    private static Closure getSdkNode(GPathResult manifestXml, int minSdk, int targetSdk) {
        def sdkAttributes = manifestXml.'uses-sdk'.'**'*.attributes()[0] ?: [:]
        sdkAttributes['android:minSdkVersion'] = String.valueOf(minSdk)
        sdkAttributes['android:targetSdkVersion'] = String.valueOf(targetSdk)

        return {
            'uses-sdk'(sdkAttributes) {}
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
                Set<String> manifests = new HashSet<>()
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

    @Override
    <T> T getProp(Map<String, T> map, T defaultValue) {
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

    File getGenPath(String... paths) {
        return rootProject.file(Paths.get(genDir, paths).toFile())
    }

    RuleType getRuleType() {
        if (isKotlin) {
            return RuleType.KOTLIN_ANDROID_LIBRARY
        } else {
            return RuleType.ANDROID_LIBRARY
        }
    }

    RuleType getTestRuleType() {
        if (isKotlin) {
            return RuleType.KOTLIN_ROBOLECTRIC_TEST
        } else {
            return RuleType.ROBOLECTRIC_TEST
        }
    }

    Set<File> getSources(BaseVariant variant) {
        ImmutableSet.Builder srcs = new ImmutableSet.Builder()

        Set<File> javaSrcs = variant.sourceSets.collect { SourceProvider provider ->
            provider.javaDirectories
        }.flatten() as Set<File>
        srcs.addAll(javaSrcs)

        if (isKotlin) {
            srcs.addAll(javaSrcs.findAll {
                it.name == "java"
            }.collect {
                project.file(it.absolutePath.replaceFirst("/java\$", "/kotlin"))
            })
        }
        return srcs.build()
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
