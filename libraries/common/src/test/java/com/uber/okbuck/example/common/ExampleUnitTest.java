package com.uber.okbuck.example.common;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.uber.okbuck.BuckRobolectricTestRunner;

import org.robolectric.annotation.Config;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;

@RunWith(BuckRobolectricTestRunner.class)
@Config(sdk = 21, packageName = "com.uber.okbuck.example.common")
public class ExampleUnitTest {

    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void resource_loading() {
        Context context = (Context) RuntimeEnvironment.application;
        assertEquals(context.getResources().getString(R.string.app_name), "Common");
    }
}
