package com.uber.okbuck.example.dummylibrary;

import android.content.Context;
import android.widget.Toast;
import com.google.gson.GsonBuilder;

public class DummyAndroidClass {
  public String getAndroidWord(Context context) {
    Toast.makeText(context, "getAndroidWord: Dummy", Toast.LENGTH_SHORT).show();
    String mock = "{\"lang\":\"" + context.getString(R.string.dummy_library_android_str) + "\"}";
    return new GsonBuilder().create().fromJson(mock, DummyObject.class).lang;
  }

  private static class DummyObject {
    private String lang;
  }
}
