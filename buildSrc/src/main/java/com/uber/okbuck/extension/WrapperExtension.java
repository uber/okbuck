package com.uber.okbuck.extension;

import com.google.common.collect.Sets;

import java.util.Set;

public class WrapperExtension {

    /**
     * Custom buck repository to add as a remote to the wrapper buck installation
     */
    public String repo = "";

    /**
     * List of files to remove when generating configuration.
     */
    public Set<String> remove = Sets.newHashSet(".buckconfig.local", "**/BUCK");

    /**
     * List of files to leave untouched when generating configuration.
     */
    public Set<String> keep = Sets.newHashSet(".okbuck/**/BUCK");

    /**
     * List of changed files to trigger okbuck runs on
     */
    public Set<String> watch = Sets.newHashSet(
            "**/*.gradle",
            "**/src/**/AndroidManifest.xml",
            "**/gradle-wrapper.properties",
            "**/lint.xml");

    /**
     * List of added/removed directories to trigger okbuck runs on
     */
    public Set<String> sourceRoots = Sets.newHashSet(
            "**/src/**/java",
            "**/src/**/res",
            "**/src/**/resources");
}
