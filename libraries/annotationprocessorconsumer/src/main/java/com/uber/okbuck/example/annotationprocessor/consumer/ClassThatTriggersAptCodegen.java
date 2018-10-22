package com.uber.okbuck.example.annotationprocessor.consumer;

import com.uber.okbuck.example.annotationprocessor.DummyAnnotation;

@DummyAnnotation
public class ClassThatTriggersAptCodegen {

  public ClassThatTriggersAptCodegen() {
    new ClassThatTriggersAptCodegen_DummyGenerated();
  }
}
