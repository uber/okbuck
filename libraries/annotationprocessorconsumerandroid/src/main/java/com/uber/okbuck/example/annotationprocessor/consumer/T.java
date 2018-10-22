package com.uber.okbuck.example.annotationprocessor.consumer;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class T {
  abstract String text();

  static void test() {
    new AutoValue_T("text");
    new AutoValue_T_DummyGeneratedForGenerated();
  }
}
