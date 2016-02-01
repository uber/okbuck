/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Piasy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.piasy.okbuck.composer

import com.android.build.gradle.internal.dsl.ProductFlavor
import com.android.builder.model.BuildType
import com.android.builder.model.ClassField
import com.github.piasy.okbuck.helper.ProjectHelper
import com.github.piasy.okbuck.rules.AndroidBuildConfigRule
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

public final class AndroidBuildConfigRuleComposer {
    private static Logger logger = Logging.getLogger(AndroidBuildConfigRuleComposer)

    private AndroidBuildConfigRuleComposer() {
        // no instance
    }

    public static AndroidBuildConfigRule compose(
            String ruleName, Project project, String flavor, String variant, String resPackage
    ) {
        return new AndroidBuildConfigRule(ruleName, Arrays.asList("PUBLIC"), resPackage,
                getBuildConfigField(project, flavor, variant))
    }

    /**
     * contract:
     * android library/app module with flavor: default + flavor + variant, latter overwrite former.
     *                         without flavor: default + release
     * */
    private static List<String> getBuildConfigField(Project project, String flavor, String variant) {
        Map<String, String> buildConfigs = new HashMap<>()
        switch (ProjectHelper.getSubProjectType(project)) {
            case ProjectHelper.ProjectType.AndroidAppProject:
            case ProjectHelper.ProjectType.AndroidLibProject:
                try {
                    project.extensions.getByName("android").metaPropertyValues.each { prop ->
                        // defaultConfig
                        if ("defaultConfig".equals(prop.name)) {
                            ProductFlavor defaultConfigs = (ProductFlavor) prop.value
                            if (defaultConfigs.applicationId != null) {
                                buildConfigs.put("APPLICATION_ID",
                                        "String APPLICATION_ID = \"${defaultConfigs.applicationId}\"")
                            }
                            buildConfigs.put("BUILD_TYPE", "String BUILD_TYPE = \"${variant}\"")
                            buildConfigs.put("FLAVOR", "String FLAVOR = \"${flavor}\"")
                            if (defaultConfigs.versionCode != null) {
                                buildConfigs.put("VERSION_CODE",
                                        "int VERSION_CODE = ${defaultConfigs.versionCode}")
                            }
                            if (defaultConfigs.versionName != null) {
                                buildConfigs.put("VERSION_NAME",
                                        "String VERSION_NAME = \"${defaultConfigs.versionName}\"")
                            }

                            for (ClassField classField : defaultConfigs.buildConfigFields.values()) {
                                buildConfigs.put(classField.name,
                                        "${classField.type} ${classField.name} = ${classField.value}")
                            }
                        }

                        // flavor
                        if ("productFlavors".equals(prop.name) && !"default".equals(flavor)) {
                            for (ProductFlavor productFlavor :
                                    ((NamedDomainObjectContainer<ProductFlavor>) prop.value).asList()) {
                                if (productFlavor.name.equals(flavor)) {
                                    for (ClassField classField : productFlavor.buildConfigFields.values()) {
                                        buildConfigs.put(classField.name,
                                                "${classField.type} ${classField.name} = ${classField.value}")
                                    }
                                }
                            }
                        }

                        // variant
                        if ("buildTypes".equals(prop.name)) {
                            for (BuildType buildType :
                                    ((NamedDomainObjectContainer<BuildType>) prop.value).asList()) {
                                if (buildType.name.equals(variant)) {
                                    for (ClassField classField : buildType.buildConfigFields.values()) {
                                        buildConfigs.put(classField.name,
                                                "${classField.type} ${classField.name} = ${classField.value}")
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.info "get ${project.name}'s build config fail"
                }
                break
            default:
                break
        }
        return buildConfigs.values().asList()
    }
}