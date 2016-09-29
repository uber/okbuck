package com.uber.okbuck.example.javalib;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class DummyTestJavaClass {

    @Test
    public void testAssertFalse() {
        assertFalse("failure - should be false", false);
    }

}

