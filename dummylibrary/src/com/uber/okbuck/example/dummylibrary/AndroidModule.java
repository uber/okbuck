package com.uber.okbuck.example.dummylibrary;

import dagger.Module;
import dagger.Provides;

@Module
public class AndroidModule {
  @Provides
  DummyAndroidClass provideDummyAndroidClass() {
    return new DummyAndroidClass();
  }
}
