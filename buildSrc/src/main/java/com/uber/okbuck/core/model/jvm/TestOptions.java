package com.uber.okbuck.core.model.jvm;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class TestOptions {

  private final List<String> jvmArgs;
  private final TreeMap<String, Object> env;

  public TestOptions(List<String> jvmArgs, Map<String, Object> env) {
    this.jvmArgs = jvmArgs;
    this.env = new TreeMap<>(env);
  }

  public List<String> getJvmArgs() {
    return jvmArgs;
  }

  public Map<String, Object> getEnv() {
    return env;
  }
}
