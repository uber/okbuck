package com.uber.okbuck.core.model.android

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.LibraryVariant
import com.android.builder.model.SourceProvider
import org.gradle.api.Project

/**
 * An Android library instrumentation target
 */
class AndroidLibInstrumentationTarget extends AndroidAppInstrumentationTarget {

    AndroidLibInstrumentationTarget(Project project, String name) {
        super(project, name)
    }

    @Override
    protected BaseVariant getBaseVariant() {
        return ((LibraryVariant) project.android.libraryVariants.find {
            it.name == getMainTargetName(name)
        }).testVariant
    }

    @Override
    Map<String, String> getResVariantDirs() {
        Map<String, String> variantDirs = new HashMap<>()
        // test variant
        List<SourceProvider> sourceSets = (project.android.libraryVariants.find {
            it.name == getMainTargetName(name)
        } as BaseVariant).sourceSets
        List<SourceProvider> testSourceSets = baseVariant.sourceSets
        // normal variant
        sourceSets.addAll(testSourceSets)
        for (SourceProvider provider : sourceSets) {
            for (String dir : getAvailable(provider.resDirectories)) {
                variantDirs.put(dir, provider.name)
            }
        }
        return variantDirs
    }
}
