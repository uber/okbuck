package com.module2;

import com.module3.Class3;
import dagger.Module;
import dagger.Provides;

@Module(library = true)
public class DaggerModule2 {

    @Provides Class3 provideClass3() {
        return null;
    }

    @Provides Interface2 provideInterface2(Class3 class3) {
        return new Interface2() {
            @Override
            public void f() {
                class3.toString();
            }
        };
    }
}
