package com.uber.okbuck.example;

import android.widget.Toast;
import androidx.multidex.MultiDex;
import com.facebook.buck.android.support.exopackage.ExopackageApplication;
import com.uber.okbuck.example.common.Calc;
import com.uber.okbuck.example.common.CalcMonitor;

public class AppShell extends ExopackageApplication {

  private static final String APP_NAME = "com.uber.okbuck.example.MyApp";
  private final boolean mIsExopackageMode;

  public AppShell() {
    super(APP_NAME, BuildConfig.EXOPACKAGE_FLAGS != 0);
    mIsExopackageMode = BuildConfig.EXOPACKAGE_FLAGS != 0;
  }

  @Override
  protected void onBaseContextAttached() {
    if (!mIsExopackageMode) {
      MultiDex.install(this);
    }
    Calc calc = new Calc(new CalcMonitor(this));
    Toast.makeText(this, "calc: " + calc.add(1, 2), Toast.LENGTH_SHORT).show();
  }
}
