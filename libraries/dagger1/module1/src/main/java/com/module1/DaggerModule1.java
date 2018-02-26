package com.module2;

import com.module2.DaggerModule2;
import com.module2.Class2;
import dagger.Module;
import dagger.Provides;

@Module(includes = DaggerModule2.class, library = true)
public class DaggerModule1 {

    @Provides Object provideObject(Class2 class2) {
        return null;
    }
}
