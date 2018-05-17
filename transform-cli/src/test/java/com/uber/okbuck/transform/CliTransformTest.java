package com.uber.okbuck.transform;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

public class CliTransformTest {

  @Before
  public void setup() throws Exception {}

  @Test(expected = IllegalArgumentException.class)
  public void whenStarting_withOneArgument_shouldThrowException() throws Exception {
    CliTransform.main(new String[] {"any"});
  }

  @Test
  public void whenStarting_shouldRunTransform() throws Exception {
    final TransformRunner runner = mock(TransformRunner.class);
    CliTransform.main(
        new CliTransform.TransformRunnerProvider() {
          @Override
          public TransformRunner provide() {
            return runner;
          }
        });
    verify(runner).runTransform();
  }
}
