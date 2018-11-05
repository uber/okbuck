package com.uber.okbuck.example;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.facebook.buck.android.support.exopackage.DefaultApplicationLike;
import com.github.promeg.xlog_android.lib.XLogConfig;
import com.squareup.leakcanary.LeakCanary;

public class MyApp extends DefaultApplicationLike {

  static {
    AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
  }

  private final Application mApplication;

  public MyApp(Application application) {
    mApplication = application;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    if (BuildConfig.XLOG_ENABLED) {
      XLogConfig.config(XLogConfig.newConfigBuilder(mApplication).build());
    }
    LeakCanary.install(mApplication);
  }
}
