package com.ubercab.transform.builder;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.build.api.transform.Context;
import com.android.build.api.transform.SecondaryInput;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;

import org.gradle.api.logging.LoggingManager;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

/**
 * A builder for {@link TransformInvocation}.
 */
public class TransformInvocationBuilder {

    @NonNull private final LinkedList<TransformInput> inputs;
    @NonNull private final LinkedList<TransformInput> referencedInputs;
    @Nullable private TransformOutputProvider outputProvider;

    /**
     * Constructor.
     */
    public TransformInvocationBuilder() {
        this.inputs = new LinkedList<>();
        this.referencedInputs = new LinkedList<>();
    }

    /**
     * Adds a new input.
     *
     * @param transformInput the input to add.
     * @return this instance of the builder
     */
    @NonNull
    public TransformInvocationBuilder addInput(@NonNull TransformInput transformInput) {
        this.inputs.add(transformInput);
        return this;
    }

    /**
     * Adds a new input.
     *
     * @param transformInput the input to add.
     * @return this instance of the builder
     */
    @NonNull
    public TransformInvocationBuilder addReferencedInput(@NonNull TransformInput transformInput) {
        this.referencedInputs.add(transformInput);
        return this;
    }

    /**
     * Sets the output provider.
     *
     * @param outputProvider the output provider.
     * @return this instance of the builder
     */
    @NonNull
    public TransformInvocationBuilder setOutputProvider(@NonNull TransformOutputProvider outputProvider) {
        this.outputProvider = outputProvider;
        return this;
    }

    /**
     * Builds the {@link TransformInvocation}.
     *
     * @return a new {@link TransformInvocation} with the specified inputs.
     */
    @NonNull
    public TransformInvocation build() {
        if (outputProvider == null) {
            throw new IllegalArgumentException("Output provider needs to be specified.");
        }
        return new CustomTransformInvocation(inputs, referencedInputs, outputProvider);
    }

    /**
     * The {@link TransformInvocation} built by the builder.
     */
    private static class CustomTransformInvocation implements TransformInvocation, Context {

        @NonNull
        private final Collection<TransformInput> inputs;
        @NonNull
        private final Collection<TransformInput> referencedInputs;
        @NonNull
        private final TransformOutputProvider transformOutputProvider;

        public CustomTransformInvocation(
                @NonNull Collection<TransformInput> inputs,
                @NonNull Collection<TransformInput> referencedInputs,
                @NonNull TransformOutputProvider transformOutputProvider) {
            this.inputs = inputs;
            this.referencedInputs = referencedInputs;
            this.transformOutputProvider = transformOutputProvider;
        }

        @Override
        @NonNull
        public Collection<TransformInput> getInputs() {
            return inputs;
        }

        @Override
        @NonNull
        public Collection<TransformInput> getReferencedInputs() {
            return referencedInputs;
        }

        @Override
        @NonNull
        public TransformOutputProvider getOutputProvider() {
            return transformOutputProvider;
        }

        @Override
        public boolean isIncremental() {
            return false;
        }

        @Override
        @NonNull
        public Context getContext() {
            return this;
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
    }
}
