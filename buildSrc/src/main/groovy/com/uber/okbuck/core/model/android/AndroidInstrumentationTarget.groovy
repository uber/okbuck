package com.uber.okbuck.core.model.android

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.android.builder.core.VariantType
import com.google.common.collect.ImmutableSet
import com.uber.okbuck.core.model.base.Scope
import org.gradle.api.Project
/**
 * An Android instrumentation target
 */
class AndroidInstrumentationTarget extends AndroidAppTarget {

    AndroidInstrumentationTarget(Project project, String name) {
        super(project, name, true)
    }

    @Override
    protected BaseVariant getBaseVariant() {
        return ((ApplicationVariant) project.android.applicationVariants.find {
            it.name == getMainTargetName(name)
        }).testVariant
    }

    @Override
    Scope getApt() {
        return Scope.from(project, expand(aptConfigs, VariantType.ANDROID_TEST.prefix))
    }

    @Override
    Scope getProvided() {
        return Scope.from(project, expand(providedConfigs, VariantType.ANDROID_TEST.prefix))
    }

    @Override
    Scope getMain() {
        return Scope.from(
                project,
                expand(compileConfigs, VariantType.ANDROID_TEST.prefix),
                getSources(baseVariant),
                null,
                getJavaCompilerOptions(baseVariant))
    }

    @Override
    Scope getTest() {
        return Scope.from(project, ImmutableSet.of())
    }

    Scope getInstrumentation() {
        return Scope.from(
                project,
                expand(compileConfigs, VariantType.ANDROID_TEST.prefix)
                        + ["androidTest${getMainTargetName(name).capitalize()}Compile"],
                getSources(baseVariant),
                null,
                getJavaCompilerOptions(instrumentationTestVariant))
    }


    static String getMainTargetName(String name) {
        return name.replaceFirst(/_test$/, '')
    }

    static String getInstrumentationTargetName(String name) {
        return "${name}_test"
    }
}
