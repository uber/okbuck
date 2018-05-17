package com.uber.okbuck.transform;

import static org.assertj.core.api.Assertions.assertThat;

import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TransformInvocationBuilderTest {

  @Mock private TransformInput transformInput;
  @Mock private TransformOutputProvider outputProvider;

  private TransformInvocationBuilder builder;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    builder = new TransformInvocationBuilder();
  }

  @Test
  public void addInput_shouldAddAnInput() throws Exception {
    TransformInvocation invocation =
        builder.addInput(transformInput).setOutputProvider(outputProvider).build();
    assertThat(invocation.getInputs()).containsExactly(transformInput);
    assertThat(invocation.getReferencedInputs()).isEmpty();
    assertThat(invocation.getOutputProvider()).isEqualTo(outputProvider);
  }

  @Test
  public void addReferencedInput_shouldAddReferencedInput() throws Exception {
    TransformInvocation invocation =
        builder.addReferencedInput(transformInput).setOutputProvider(outputProvider).build();
    assertThat(invocation.getInputs()).isEmpty();
    assertThat(invocation.getReferencedInputs()).containsExactly(transformInput);
    assertThat(invocation.getOutputProvider()).isEqualTo(outputProvider);
  }

  @Test(expected = IllegalArgumentException.class)
  public void whenOutputProviderNotSet_shouldThrowException() throws Exception {
    builder.build();
  }
}
