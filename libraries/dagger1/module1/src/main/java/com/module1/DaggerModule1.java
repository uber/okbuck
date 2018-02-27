package com.module1;

import com.module2.DaggerModule2;
import com.module2.Interface2;
import dagger.Module;
import dagger.Provides;

@Module(includes = DaggerModule2.class, library = true)
public class DaggerModule1 {

    @Provides Object provideObject(Interface2 interface2) {
        return null;
    }
}
