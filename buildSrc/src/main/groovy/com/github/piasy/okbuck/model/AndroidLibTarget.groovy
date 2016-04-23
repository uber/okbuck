package com.github.piasy.okbuck.model

import com.android.build.gradle.api.BaseVariant
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
}
