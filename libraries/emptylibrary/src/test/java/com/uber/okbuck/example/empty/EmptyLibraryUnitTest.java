package com.uber.okbuck.example.empty;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27, packageName = "com.uber.okbuck.example.empty")
public class EmptyLibraryUnitTest {

    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void resource_loading() {
        assertEquals(RuntimeEnvironment.application.getResources().getString(R.string.empty_release_string), "empty_release_string");
    }
}
