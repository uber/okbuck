package com.module2;

import com.module3.Class3;
import dagger.Module;
import dagger.Provides;

@Module(library = true)
public class DaggerModule2 {

    @Provides Class3 provideClass3() {
        return null;
    }

    @Provides Class2 provideClass2(Class3 class3) {
        return null;
    }
}
