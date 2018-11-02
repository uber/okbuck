package com.uber.okbuck.extension;

import com.uber.okbuck.core.manager.LintManager;

import org.gradle.api.Project;

import javax.annotation.Nullable;

public class JetifierExtension {

    public static final String JETIFIER_VERSION = "1.0.0-beta02";

    /** Jetifier jar version */
    public String version;

    JetifierExtension() {
        version = JETIFIER_VERSION;
    }
}
