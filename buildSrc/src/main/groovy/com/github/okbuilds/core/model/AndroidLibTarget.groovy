package com.github.okbuilds.core.model

import com.android.build.gradle.api.BaseVariant
import groovy.util.slurpersupport.GPathResult
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
    protected void manipulateManifest(GPathResult manifest) {
        // nothing need be done right now
    }
}
