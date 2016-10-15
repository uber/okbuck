package com.uber.okbuck.core.model

import com.android.build.gradle.api.BaseVariant
import com.android.manifmerger.ManifestMerger2
import org.gradle.api.Project

/**
 * An Android library target
 */
class AndroidLibTarget extends AndroidTarget {

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
}
