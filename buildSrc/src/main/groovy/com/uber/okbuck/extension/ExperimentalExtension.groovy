package com.uber.okbuck.extension

import com.uber.okbuck.core.annotation.Experimental

@Experimental
class ExperimentalExtension {

    /**
     * Whether lint rules are to be generated
     */
    boolean lint = false

    /**
     * Whether retrolambda is enabled
     */
    boolean retrolambda = false

    /**
     * Whether transform rules are to be generated
     */
    boolean transform = false

    /**
     * Generate buck files per project in parallel sub tasks
     */
    boolean parallel = false
}
