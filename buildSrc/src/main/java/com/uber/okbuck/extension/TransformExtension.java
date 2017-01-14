package com.uber.okbuck.extension;

import com.uber.okbuck.core.annotation.Experimental;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@Experimental
public class TransformExtension {

    /**
     * Stores the configuration per transform. Mapping is stored as target-[transforms].
     */
    public Map<String, Map<String, String>> transforms = new HashMap<>();

    /**
     * Transform runner class
     */
    public String main;
}
