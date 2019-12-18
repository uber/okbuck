package com.uber.okbuck.core.annotation;

import com.google.auto.value.AutoValue;
import com.uber.okbuck.core.dependency.OExternalDependency;
import com.uber.okbuck.core.model.base.Target;
import java.util.Optional;

@AutoValue
public abstract class JvmPlugin {

  public abstract String pluginUID();

  public abstract Optional<OExternalDependency> pluginDependency();

  public abstract Optional<Target> pluginTarget();

  public static Builder builder() {
    return new AutoValue_JvmPlugin.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setPluginUID(String value);

    public abstract Builder setPluginDependency(Optional<OExternalDependency> value);

    public abstract Builder setPluginDependency(OExternalDependency value);

    public abstract Builder setPluginTarget(Optional<Target> value);

    public abstract Builder setPluginTarget(Target value);

    public abstract JvmPlugin build();
  }
}
