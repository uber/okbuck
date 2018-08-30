package com.uber.okbuck.example.lintError;

import android.util.Log;
import java.util.Arrays;
import java.util.function.Consumer;

public class MyClass {

  public void testCustomLint_fromAar_shouldFail() {
    int dimen = R2.dimen.app_bar_height;
  }

  public void testCustomLint_usingSystemTime_shouldFail() {
    System.currentTimeMillis();
  }

  public void testNewApiSdkLint_usingStreamsApi_shouldFail() {
    Arrays.asList(1, 2, 3)
        .stream()
        .forEach(
            new Consumer<Integer>() {
              @Override
              public void accept(Integer num) {
                Log.d("TAG", "number is: " + String.valueOf(num));
              }
            });
  }
}
