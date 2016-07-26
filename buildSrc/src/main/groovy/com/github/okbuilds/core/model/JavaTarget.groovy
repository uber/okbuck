package com.github.okbuilds.core.model

import groovy.transform.Memoized
import org.apache.commons.io.IOUtils
import org.gradle.api.JavaVersion
import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile

abstract class JavaTarget extends Target {

    JavaTarget(Project project, String name) {
        super(project, name)
    }

    /**
     * Main Scope
     */
    abstract Scope getMain()

    /**
     * Test Scope
     */
    abstract Scope getTest()

    /**
     * Apt Scope
     */
    Scope getApt() {
        Scope aptScope = new Scope(project, ["apt", "provided", 'compileOnly'])
        aptScope.targetDeps.retainAll(aptScope.targetDeps.findAll { Target target ->
            target.getProp(okbuck.annotationProcessors, null) != null
        })
        return aptScope
    }

    /**
     * List of annotation processor classes.
     */
    Set<String> getAnnotationProcessors() {
        return (apt.externalDeps.collect { String aptDep ->
            JarFile jar = new JarFile(new File(aptDep))
            jar.entries().findAll { JarEntry entry ->
                (entry.name == "META-INF/services/javax.annotation.processing.Processor")
            }.collect { JarEntry aptEntry ->
                IOUtils.toString(jar.getInputStream(aptEntry)).trim().split("\\n")
            }
        } + apt.targetDeps.collect { Target target ->
            (List<String>) target.getProp(okbuck.annotationProcessors, null)
        }.findAll { List<String> processors ->
            processors != null
        }).flatten() as List<String>
    }

    protected static String javaVersion(JavaVersion version) {
        switch (version) {
            case JavaVersion.VERSION_1_6:
                return '6'
            case JavaVersion.VERSION_1_7:
                return '7'
            case JavaVersion.VERSION_1_8:
                return '8'
            case JavaVersion.VERSION_1_9:
                return '9'
            default:
                return '7'
        }
    }
}
