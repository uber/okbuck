package com.uber.okbuck.example.javalib;

import dagger.Module;
import dagger.Provides;

@Module
public class JavaModule {
  @Provides
  DummyJavaClass provideDummyJavaClass() {
    return new DummyJavaClass();
  }
}
