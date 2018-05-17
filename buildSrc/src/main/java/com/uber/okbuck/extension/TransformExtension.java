package com.uber.okbuck.extension;

import com.uber.okbuck.core.annotation.Experimental;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@Experimental
public class TransformExtension {

  /** Stores the configuration per transform. Mapping is stored as target-[transforms]. */
  public Map<String, List<Map<String, String>>> transforms = new HashMap<>();
}
