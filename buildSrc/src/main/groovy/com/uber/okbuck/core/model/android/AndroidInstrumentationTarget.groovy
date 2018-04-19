package com.uber.okbuck.core.model.android

import com.uber.okbuck.core.model.base.AnnotationProcessorCache
import com.uber.okbuck.core.model.base.Scope
import com.uber.okbuck.core.util.ProjectUtil
import com.uber.okbuck.extension.ExperimentalExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 * An abstract Android instrumentation target
 */
abstract class AndroidInstrumentationTarget extends AndroidAppTarget {

    AndroidInstrumentationTarget(Project project, String name) {
        super(project, name, true)
    }

    // TODO: Update to use variant once issue solved: https://youtrack.jetbrains.com/issue/KT-23411
    @Override
    List<Scope> getAptScopes() {
        AnnotationProcessorCache apCache = ProjectUtil.getAnnotationProcessorCache(project)
        return apCache.getAnnotationProcessorScopes(project,
                isKapt ? "kaptAndroidTest" : baseVariant.annotationProcessorConfiguration)
    }


    @Override
    Scope getApt() {
        def configuration = isKapt ? "kaptAndroidTest" : baseVariant.annotationProcessorConfiguration

        return getAptScopeForConfiguration(configuration)
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
