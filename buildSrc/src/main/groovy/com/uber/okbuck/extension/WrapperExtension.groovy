package com.uber.okbuck.extension

class WrapperExtension {

    /**
     * Custom buck repository to add as a remote to the wrapper buck installation
     */
    String repo = ''

    /**
     * List of files to remove when generating configuration.
     */
    List<String> remove = ['.buckconfig.local', "**/BUCK"]

    /**
     * List of files to leave untouched when generating configuration.
     */
    List<String> keep = [".okbuck/**/BUCK"]
}
