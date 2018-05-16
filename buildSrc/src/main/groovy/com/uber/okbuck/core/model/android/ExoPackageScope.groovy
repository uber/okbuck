package com.uber.okbuck.core.model.android

import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.uber.okbuck.core.dependency.ExternalDependency
import com.uber.okbuck.core.model.base.Scope
import com.uber.okbuck.core.model.base.Target
import com.uber.okbuck.core.util.FileUtil
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList

import com.uber.okbuck.core.util.XmlUtil;

class ExoPackageScope extends Scope {

    private final Scope base
    private final String manifest

    ExoPackageScope(Project project, Scope base, List<String> exoPackageDependencies, String
            manifest) {
        super(project, null, ImmutableSet.of(), ImmutableSet.of(), ImmutableList.of())
        this.base = base
        this.manifest = manifest
        extractDependencies(base, exoPackageDependencies)
    }

    String getAppClass() {
        String appClass = null
        XmlSlurper slurper = new XmlSlurper()
        slurper.DTDHandler = null

        File manifestFile = project.rootProject.file(manifest)
        Document manifestXml = XmlUtil.loadXml(manifestFile)
        try {
            NodeList nodeList = manifestXml.getElementsByTagName("application")
            Preconditions.checkArgument(nodeList.length == 1)

            Element application = (Element) nodeList.item(0)

            appClass = application.getAttribute("android:name")
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

    private void extractDependencies(Scope base, List<String> exoPackageDependencies) {
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
