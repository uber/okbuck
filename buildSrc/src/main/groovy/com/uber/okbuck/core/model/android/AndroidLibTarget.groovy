package com.uber.okbuck.core.model.android

import com.android.build.gradle.api.BaseVariant
import com.android.manifmerger.ManifestMerger2
import com.google.common.collect.ImmutableList
import com.uber.okbuck.core.util.KotlinUtil
import org.gradle.api.Project

/**
 * An Android library target
 */
class AndroidLibTarget extends AndroidTarget {

    private static final String KOTLIN_EXTENSIONS_OPTION = "plugin:org.jetbrains.kotlin.android:"

    AndroidLibTarget(Project project, String name, boolean isTest = false) {
        super(project, name, isTest)
    }

    @Override
    protected BaseVariant getBaseVariant() {
        return project.android.libraryVariants.find { it.name == name } as BaseVariant
    }

    @Override
    ManifestMerger2.MergeType getMergeType() {
        return ManifestMerger2.MergeType.LIBRARY
    }

    List<String> getKotlincArguments() {
        if (!hasKotlinAndroidExtensions) {
            return ImmutableList.of()
        }

        ImmutableList.Builder<String> extraKotlincArgs = ImmutableList.<String>builder()
        StringBuilder plugin = new StringBuilder()
        StringBuilder resDirs = new StringBuilder()
        StringBuilder options = new StringBuilder()

        // :root:module -> root/module/
        String module = project.path.replace(":", File.separator).substring(1) + File.separator

        getResVariantDirs().each { String dir, String variant ->
            String pathToRes = module + dir
            resDirs.append(KOTLIN_EXTENSIONS_OPTION)
            resDirs.append("variant=")
            resDirs.append(variant)
            resDirs.append(";")
            resDirs.append(pathToRes)
            resDirs.append(",")
        }

        plugin.append("-Xplugin=")
        plugin.append(KotlinUtil.KOTLIN_HOME_LOCATION)
        plugin.append(File.separator)
        plugin.append("kotlin-android-extensions.jar")

        options.append(resDirs.toString())
        options.append(KOTLIN_EXTENSIONS_OPTION)
        options.append("package=")
        options.append(getPackage())

        if (hasExperimentalKotlinAndroidExtensions) {
            options.append(",")
            options.append(KOTLIN_EXTENSIONS_OPTION)
            options.append("experimental=true")
        }

        extraKotlincArgs.add(plugin.toString())
        extraKotlincArgs.add("-P")
        extraKotlincArgs.add(options.toString())

        return extraKotlincArgs.build()
    }
}
