package com.uber.okbuck.example.common;

import android.content.Context;
import android.graphics.Color;

import com.uber.okbuck.BuckRobolectricTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

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
        Context context = RuntimeEnvironment.application;
        int actualColor = Color.parseColor("#cccccc");
        assertEquals(context.getResources().getString(R.string.app_name), "Common");
        assertEquals(context.getResources().getColor(R.color.fooColor), actualColor);
        assertEquals(Person.getColor(context), actualColor);
    }
}
