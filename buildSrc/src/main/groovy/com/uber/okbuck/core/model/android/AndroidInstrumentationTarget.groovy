package com.uber.okbuck.core.model.android

import com.uber.okbuck.core.model.base.Scope
import org.gradle.api.Project

/**
 * An abstract Android instrumentation target
 */
abstract class AndroidInstrumentationTarget extends AndroidAppTarget {

    AndroidInstrumentationTarget(Project project, String name) {
        super(project, name, true)
    }

    // TODO: Update to use variant once issue solved: https://youtrack.jetbrains.com/issue/KT-23411
    @Override
    Scope getApt() {
        return Scope.from(project, isKapt ? "kaptAndroidTest" : baseVariant.annotationProcessorConfiguration)
    }

    @Override
    Scope getProvided() {
        return Scope.from(project, baseVariant.compileConfiguration)
    }

    @Override
    Scope getMain() {
        return Scope.from(
                project,
                baseVariant.runtimeConfiguration,
                getSources(baseVariant),
                getJavaResources(baseVariant),
                getJavaCompilerOptions(baseVariant))
    }

    @Override
    Scope getTest() {
        return Scope.from(project, null)
    }

    static String getMainTargetName(String name) {
        return name.replaceFirst(/_test$/, '')
    }

    static String getInstrumentationTargetName(String name) {
        return "${name}_test"
    }
}
