package com.uber.okbuck.example;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.facebook.buck.android.support.exopackage.DefaultApplicationLike;

public class MyApp extends DefaultApplicationLike {

  static {
    AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
  }

  public MyApp(Application application) {
  }
}
