package com.uber.okbuck.example.dummylibrary;

import android.app.Activity;
import android.os.Bundle;
import com.uber.okbuck.example.common.CalcMonitor;
import com.uber.okbuck.example.javalib.DummyJavaClass;

public class DummyActivity extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    String mock = "Mock string from DummyActivity";
    new Thread(() -> System.out.println(mock + " 1")).start();
    dummyCall(System.out::println, mock + " 2");
    CalcMonitor calcMonitor = new CalcMonitor(this);
  }

  private void dummyCall(DummyJavaClass.DummyInterface dummyInterface, String val) {
    dummyInterface.call(val);
  }
}
