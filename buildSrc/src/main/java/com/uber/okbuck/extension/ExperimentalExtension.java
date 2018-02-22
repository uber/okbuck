package com.uber.okbuck.extension;

import com.uber.okbuck.core.annotation.Experimental;

@Experimental
public class ExperimentalExtension {

    /**
     * Whether transform rules are to be generated
     */
    public boolean transform = false;

    /**
     * Whether to use classpath_abi for lint or not
     */
    public boolean lintWithClasspathAbi = false;
}
