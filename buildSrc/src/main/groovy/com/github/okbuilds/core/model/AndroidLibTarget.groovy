package com.github.okbuilds.core.model

import com.android.build.gradle.api.BaseVariant
import com.android.manifmerger.ManifestMerger2
import org.gradle.api.Project

/**
 * An Android library target
 */
class AndroidLibTarget extends AndroidTarget {

    AndroidLibTarget(Project project, String name) {
        super(project, name)
    }

    @Override
    protected BaseVariant getBaseVariant() {
        return (BaseVariant) project.android.libraryVariants.find { it.name == name }
    }

    @Override
    ManifestMerger2.MergeType getMergeType() {
        return ManifestMerger2.MergeType.LIBRARY
    }
}
