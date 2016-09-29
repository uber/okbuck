package com.uber.okbuck.example.javalib;

import dagger.Module;
import dagger.Provides;

@Module
public class JavaModule {
    @Provides
    DummyJavaClass provodeDummyJavaClass() {
        return new DummyJavaClass();
    }
}
