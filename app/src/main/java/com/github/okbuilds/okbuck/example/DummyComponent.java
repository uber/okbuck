package com.github.okbuilds.okbuck.example;

import com.github.okbuilds.okbuck.example.dummylibrary.AndroidModule;
import com.github.okbuilds.okbuck.example.javalib.JavaModule;

import dagger.Component;

@Component(modules = {JavaModule.class, AndroidModule.class})
public interface DummyComponent {
    void inject(MainActivity activity);
}
