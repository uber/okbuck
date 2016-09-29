package com.uber.okbuck

import com.uber.okbuck.core.annotation.Experimental

@Experimental
class ExperimentalExtension {

    /**
     * Enable generation of robolectric rules.
     */
    boolean robolectric = false

    /**
     * Enable fetching source jars.
     */
    boolean sources = false

    /**
     * Enable generation of espresso test rules.
     */
    boolean espresso = false
}
