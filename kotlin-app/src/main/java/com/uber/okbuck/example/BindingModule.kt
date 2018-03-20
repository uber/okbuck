package com.uber.okbuck.example

import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class BindingModule {

    @ContributesAndroidInjector
    abstract fun contributeMainActivity(): MainActivity
}
