package com.uber.okbuck.core.model.android

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.SourceProvider
import com.uber.okbuck.core.model.base.Scope
import com.uber.okbuck.core.model.base.Target
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
        Scope aptScope = new Scope(project, ["androidTestApt"])
        aptScope.targetDeps.retainAll(aptScope.targetDeps.findAll { Target target ->
            target.getProp(okbuck.annotationProcessors, null) != null
        })
        return aptScope
    }

    @Override
    Scope getMain() {
        return new Scope(
                project,
                [
                 "compile",
                 "${buildType}Compile",
                 "${flavor}Compile",
                 "${getMainTargetName(name).capitalize()}Compile",
                 "androidTestCompile",
                 "androidTest${buildType.capitalize()}Compile",
                 "androidTest${flavor.capitalize()}Compile",
                 "androidTest${getMainTargetName(name).capitalize()}Compile"] as Set,
                baseVariant.sourceSets.collect { SourceProvider provider ->
                    provider.javaDirectories
                }.flatten() as Set<File>,
                null,
                getJavaCompilerOptions(baseVariant))
    }

    @Override
    Set<String> getDepConfigNames() {
        return super.getDepConfigNames() +
                [
                 "androidTestApt",
                 "compile",
                 "${buildType}Compile",
                 "${flavor}Compile",
                 "${getMainTargetName(name).capitalize()}Compile",
                 "androidTestCompile",
                 "androidTest${buildType.capitalize()}Compile",
                 "androidTest${flavor.capitalize()}Compile",
                 "androidTest${getMainTargetName(name).capitalize()}Compile"]
    }

    Scope getInstrumentation() {
        return new Scope(
                project,
                ["androidTestCompile",
                 "androidTest${buildType.capitalize()}Compile",
                 "androidTest${flavor.capitalize()}Compile",
                 "androidTest${getMainTargetName(name).capitalize()}Compile"] as Set,
                baseVariant.sourceSets.collect { SourceProvider provider ->
                    provider.javaDirectories
                }.flatten() as Set<File>,
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
