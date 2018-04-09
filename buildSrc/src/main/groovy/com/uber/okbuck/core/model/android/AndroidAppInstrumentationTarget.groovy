package com.uber.okbuck.core.model.android

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.uber.okbuck.core.model.base.Scope
import org.gradle.api.Project

/**
 * An Android instrumentation target
 */
class AndroidAppInstrumentationTarget extends AndroidInstrumentationTarget {

    AndroidAppInstrumentationTarget(Project project, String name) {
        super(project, name)
    }

    @Override
    protected BaseVariant getBaseVariant() {
        return ((ApplicationVariant) project.android.applicationVariants.find {
            it.name == getMainTargetName(name)
        }).testVariant
    }
}
