package com.uber.okbuck.core.model.jvm;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class TestOptions {

  private final List<String> jvmArgs;
  private final Map<String, Object> env;

  public static final TestOptions EMPTY =
      new TestOptions(Collections.emptyList(), Collections.emptyMap());

  public TestOptions(List<String> jvmArgs, Map<String, Object> env) {
    this.jvmArgs = jvmArgs;
    this.env = env;
  }

  public List<String> getJvmArgs() {
    return jvmArgs;
  }

  public Map<String, Object> getEnv() {
    return env;
  }
}
