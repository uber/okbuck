package com.uber.okbuck.extension

import com.uber.okbuck.core.annotation.Experimental

@Experimental
class TransformExtension {

    /**
     * Stores the configuration per transform. Mapping is stored as target-[transforms].
     */
    Map<String, Map<String, String>> transforms = [:]

    /**
     * Transform runner class
     */
    String main
}
