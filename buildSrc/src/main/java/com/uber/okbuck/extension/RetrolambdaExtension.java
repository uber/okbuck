package com.uber.okbuck.extension;

import com.uber.okbuck.core.annotation.Experimental;

@SuppressWarnings("CanBeFinal")
@Experimental
public class RetrolambdaExtension {

    /**
     * Retrolambda version. Defaults to '2.3.0'
     */
    public String version = "2.5.0";

    /**
     * Jvm arguments when running retrolambda
     */
    public String jvmArgs = "";
}
