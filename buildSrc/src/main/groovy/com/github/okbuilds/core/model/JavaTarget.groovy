package com.github.okbuilds.core.model

import com.github.okbuilds.core.dependency.ExternalDependency
import com.github.okbuilds.okbuck.OkBuckExtension
import org.apache.commons.io.IOUtils
import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile

abstract class JavaTarget extends Target {

    static final Set<String> APT_CONFIGURATIONS = ["apt", "provided", 'compileOnly'] as Set
    static final String PROCESSOR_ENTRY =
            "META-INF/services/javax.annotation.processing.Processor"

    final Set<String> sources = [] as Set
    final Set<String> testSources = [] as Set
    final Set<JavaTarget> targetAptDeps = [] as Set
    final Set<JavaTarget> targetCompileDeps = [] as Set
    final Set<JavaTarget> targetTestCompileDeps = [] as Set

    protected final Set<ExternalDependency> externalAptDeps = [] as Set
    protected final Set<ExternalDependency> externalCompileDeps = [] as Set
    protected final Set<ExternalDependency> externalTestCompileDeps = [] as Set

    /**
     * Constructor.
     *
     * @param project The project.
     * @param name The target name.
     */
    JavaTarget(Project project, String name) {
        super(project, name)

        sources.addAll(getAvailable(sourceDirs()))
        testSources.addAll(getAvailable(testSourceDirs()))

        extractConfigurations(APT_CONFIGURATIONS, externalAptDeps, targetAptDeps)
        OkBuckExtension okbuck = rootProject.okbuck
        targetAptDeps.retainAll(targetAptDeps.findAll { JavaTarget target ->
            target.getProp(okbuck.annotationProcessors, null) != null
        })

        extractConfigurations(compileConfigurations(), externalCompileDeps, targetCompileDeps)
        extractConfigurations(testCompileConfigurations(), externalTestCompileDeps, targetTestCompileDeps)
    }

    /**
     * List of source directories.
     */
    protected abstract Set<File> sourceDirs()

    /**
     * List of test source directories.
     */
    protected abstract Set<File> testSourceDirs()

    /**
     * List of compile configurations.
     */
    protected abstract Set<String> compileConfigurations()

    /**
     * List of test compile configurations.
     */
    protected abstract Set<String> testCompileConfigurations()

    Set<String> getAnnotationProcessors() {
        OkBuckExtension okbuck = rootProject.okbuck
        return aptDeps.collect { String aptDep ->
            JarFile jar = new JarFile(new File(aptDep))
            jar.entries().findAll { JarEntry entry ->
                entry.name.equals(PROCESSOR_ENTRY)
            }.collect { JarEntry aptEntry ->
                IOUtils.toString(jar.getInputStream(aptEntry)).trim().split("\\n")
            }
        }.plus(targetAptDeps.collect { JavaTarget target ->
            (List<String>) target.getProp(okbuck.annotationProcessors, null)
        }.findAll { List<String> processors ->
            processors != null
        }).flatten() as List<String>
    }

    Set<String> getCompileDeps() {
        externalCompileDeps.collect { ExternalDependency dependency ->
            dependencyCache.get(dependency)
        }
    }

    Set<String> getTestCompileDeps() {
        externalTestCompileDeps.collect { ExternalDependency dependency ->
            dependencyCache.get(dependency)
        }
    }

    Set<String> getAptDeps() {
        externalAptDeps.collect { ExternalDependency dependency ->
            dependencyCache.get(dependency)
        }
    }
}
