package com.uber.okbuck.extension;

import com.uber.okbuck.core.util.KotlinUtil;

import org.gradle.api.Project;

public class KotlinExtension {

    /**
     * Version of the kotlin compiler to use.
     */
    @SuppressWarnings("CanBeFinal")
    public String version;

    public KotlinExtension(Project project) {
        version = KotlinUtil.getDefaultKotlinVersion(project);
    }
}
