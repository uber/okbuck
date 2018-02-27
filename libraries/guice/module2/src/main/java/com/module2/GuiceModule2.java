package com.module2;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.module3.Class3;


public class GuiceModule2 extends AbstractModule {

    @Override
    protected void configure() {
        bind(Interface2.class).to(Interface2Impl.class);
    }

    private static class Interface2Impl implements Interface2 {

        @Inject
        private Class3 class3;

        @Override
        public String f() {
            return class3.toString();
        }
    }
}
