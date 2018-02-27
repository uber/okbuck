package com.module1;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.module2.GuiceModule2;
import com.module2.Interface2;

public class GuiceApp {

    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new GuiceModule2());
        Interface2 interface2 = injector.getInstance(Interface2.class);

        System.out.println(interface2.f());
    }
}
