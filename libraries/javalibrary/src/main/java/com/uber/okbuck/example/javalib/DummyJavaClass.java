package com.uber.okbuck.example.javalib;

import com.google.gson.GsonBuilder;

public class DummyJavaClass {
  public String getJavaWord() {
    String mock = "{\"lang\":\"Java\"}";
    final String mock2 = "Mock string from DummyJavaClass";
    new Thread(() -> System.out.println(mock2 + " 1")).start();
    dummyCall(System.out::println, mock2 + " 2");
    return new GsonBuilder().create().fromJson(mock, DummyObject.class).lang;
  }

  private void dummyCall(DummyInterface dummyInterface, String val) {
    dummyInterface.call(val);
  }

  public interface DummyInterface {
    void call(String v);
  }

  private static class DummyObject {
    private String lang;
  }
}
