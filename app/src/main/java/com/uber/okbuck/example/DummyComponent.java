package com.uber.okbuck.example;

import com.uber.okbuck.example.dummylibrary.AndroidModule;
import com.uber.okbuck.example.javalib.JavaModule;
import dagger.Component;

@Component(modules = {JavaModule.class, AndroidModule.class})
public interface DummyComponent {
  void inject(MainActivity activity);
}
