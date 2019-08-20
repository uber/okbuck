package com.uber.okbuck.example.javalib;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class DummyJavaClassIntegrationTest {

  @Test
  public void testAssertFalse() {
    DummyJavaClass dummyJavaClass = new DummyJavaClass();
    assertFalse("failure - should be false", false);
  }
}
