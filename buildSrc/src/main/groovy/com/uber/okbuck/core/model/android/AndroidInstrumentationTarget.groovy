package com.uber.okbuck.core.model.android

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.android.builder.core.VariantType
import com.uber.okbuck.core.model.base.Scope
import org.gradle.api.Project
/**
 * An Android instrumentation target
 */
class AndroidInstrumentationTarget extends AndroidAppTarget {

    static final String ANDROID_TEST_PREFIX = VariantType.ANDROID_TEST.prefix

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
        return new Scope(project, expand(aptConfigs, ANDROID_TEST_PREFIX))
    }

    @Override
    Scope getProvided() {
        return new Scope(project, expand(providedConfigs, ANDROID_TEST_PREFIX))
    }

    @Override
    Scope getMain() {
        return new Scope(
                project,
                expand(compileConfigs, ANDROID_TEST_PREFIX, true),
                getSources(baseVariant),
                null,
                getJavaCompilerOptions(baseVariant))
    }

    Scope getInstrumentation() {
        return new Scope(
                project,
                expand(compileConfigs, ANDROID_TEST_PREFIX)
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
