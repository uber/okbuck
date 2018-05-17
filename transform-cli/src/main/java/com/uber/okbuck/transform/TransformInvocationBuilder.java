package com.uber.okbuck.transform;

import com.android.build.api.transform.Context;
import com.android.build.api.transform.SecondaryInput;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import org.gradle.api.logging.LoggingManager;
import org.gradle.workers.WorkerExecutor;

/** A builder for {@link TransformInvocation}. */
class TransformInvocationBuilder {

  private final LinkedList<TransformInput> inputs;
  private final LinkedList<TransformInput> referencedInputs;
  private TransformOutputProvider outputProvider;

  /** Constructor. */
  TransformInvocationBuilder() {
    this.inputs = new LinkedList<>();
    this.referencedInputs = new LinkedList<>();
  }

  /**
   * Adds a new input.
   *
   * @param transformInput the input to add.
   * @return this instance of the builder
   */
  TransformInvocationBuilder addInput(TransformInput transformInput) {
    this.inputs.add(transformInput);
    return this;
  }

  /**
   * Adds a new input.
   *
   * @param transformInput the input to add.
   * @return this instance of the builder
   */
  TransformInvocationBuilder addReferencedInput(TransformInput transformInput) {
    this.referencedInputs.add(transformInput);
    return this;
  }

  /**
   * Sets the output provider.
   *
   * @param outputProvider the output provider.
   * @return this instance of the builder
   */
  TransformInvocationBuilder setOutputProvider(TransformOutputProvider outputProvider) {
    this.outputProvider = outputProvider;
    return this;
  }

  /**
   * Builds the {@link TransformInvocation}.
   *
   * @return a new {@link TransformInvocation} with the specified inputs.
   */
  TransformInvocation build() {
    if (outputProvider == null) {
      throw new IllegalArgumentException("Output provider needs to be specified.");
    }
    return new CustomTransformInvocation(inputs, referencedInputs, outputProvider);
  }

  /** The {@link TransformInvocation} built by the builder. */
  private static class CustomTransformInvocation implements TransformInvocation, Context {

    private final Collection<TransformInput> inputs;

    private final Collection<TransformInput> referencedInputs;

    private final TransformOutputProvider transformOutputProvider;

    CustomTransformInvocation(
        Collection<TransformInput> inputs,
        Collection<TransformInput> referencedInputs,
        TransformOutputProvider transformOutputProvider) {
      this.inputs = inputs;
      this.referencedInputs = referencedInputs;
      this.transformOutputProvider = transformOutputProvider;
    }

    @Override
    public Context getContext() {
      return this;
    }

    @Override
    public Collection<TransformInput> getInputs() {
      return inputs;
    }

    @Override
    public Collection<TransformInput> getReferencedInputs() {
      return referencedInputs;
    }

    @Override
    public boolean isIncremental() {
      return false;
    }

    @Override
    public TransformOutputProvider getOutputProvider() {
      return transformOutputProvider;
    }

    @Override
    public Collection<SecondaryInput> getSecondaryInputs() {
      throw new UnsupportedOperationException();
    }

    @Override
    public LoggingManager getLogging() {
      throw new UnsupportedOperationException();
    }

    @Override
    public File getTemporaryDir() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getPath() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getVariantName() {
      throw new UnsupportedOperationException();
    }

    @Override
    public WorkerExecutor getWorkerExecutor() {
      throw new UnsupportedOperationException();
    }
  }
}
