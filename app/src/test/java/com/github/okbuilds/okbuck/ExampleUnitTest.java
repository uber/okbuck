package com.github.okbuilds.okbuck;

import com.github.okbuilds.okbuck.example.common.Calc;
import com.github.okbuilds.okbuck.example.common.CalcMonitor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    @Test
    public void testAdd() throws Exception {
        CalcMonitor monitor = mock(CalcMonitor.class);
        Calc calc = new Calc(monitor);

        int ret = calc.add(1, 2);
        verify(monitor, only()).addCalled(anyString());
        assertEquals(3, ret);
    }
}
