package com.uber.okbuck.example;

import android.content.Context;

public class FlavorLogger {

  public static String log(Context context) {
    return "FlavorLogger, dev, " + context.getString(R.string.flavor_string);
  }
}
