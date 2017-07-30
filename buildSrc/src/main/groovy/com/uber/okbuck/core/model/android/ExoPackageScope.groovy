package com.uber.okbuck.core.model.android

import com.uber.okbuck.core.dependency.ExternalDependency
import com.uber.okbuck.core.model.base.Scope
import com.uber.okbuck.core.model.base.Target
import com.uber.okbuck.core.util.FileUtil
import groovy.util.slurpersupport.GPathResult
import org.gradle.api.Project
import org.gradle.api.file.FileTree

class ExoPackageScope extends Scope {

    private final Scope base
    private final String manifest

    ExoPackageScope(Project project, Scope base, Set<String> exoPackageDependencies, String manifest) {
        super(project, [])
        this.base = base
        this.manifest = manifest
        extractDependencies(base, exoPackageDependencies)
    }

    String getAppClass() {
        String appClass = null
        XmlSlurper slurper = new XmlSlurper()
        slurper.DTDHandler = null
        GPathResult manifestXml = slurper.parse(project.rootProject.file(manifest))
        try {
            appClass = manifestXml.application.@"android:name"
            appClass = appClass.replaceAll('\\.', "/").replaceAll('^/', '')
        } catch (Exception ignored) {}
        if (appClass != null && !appClass.empty) {
            base.sources.each { String sourceDir ->
                FileTree found = project.fileTree(dir: sourceDir, includes: ["**/${appClass}.java"])
                if (found.size() == 1) {
                    appClass = FileUtil.getRelativePath(project.projectDir, found[0])
                }
            }
        }
        return appClass
    }

    private void extractDependencies(Scope base, Set<String> exoPackageDependencies) {
        exoPackageDependencies.each { String exoPackageDep ->
            String first // can denote either group or project name
            String last // can denote either module or configuration name
            boolean fullyQualified = false

            if (exoPackageDep.contains(":")) {
                List<String> parts = exoPackageDep.split(":")
                first = parts[0]
                last = parts[1]
                fullyQualified = true
            } else {
                first = last = exoPackageDep
            }

            ExternalDependency externalDep = base.external.find { ExternalDependency dependency ->
                boolean match = true
                if (fullyQualified) {
                    match &= dependency.group == first
                }
                match &= dependency.name == last
                return match
            }

            if (externalDep != null) {
                external.add(externalDep)
            } else {
                Target variantDep = base.targetDeps.find { Target variant ->
                    boolean match = true
                    if (fullyQualified) {
                        match &= variant.name == last
                    }
                    match &= variant.path == first
                    return match
                }

                if (variantDep != null) {
                    targetDeps.add(variantDep)
                }
            }
        }
    }
}
