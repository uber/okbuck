package com.uber.okbuck.extension

import com.uber.okbuck.core.annotation.Experimental

@Experimental
class RetrolambdaExtension {

    /**
     * Retrolambda version. Defaults to '2.3.0'
     */
    String version = "2.3.0"

    /**
     * Jvm arguments when running retrolambda
     */
    String jvmArgs = ""
}
