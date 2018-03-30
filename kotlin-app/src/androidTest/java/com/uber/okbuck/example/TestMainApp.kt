package com.uber.okbuck.example

import dagger.android.AndroidInjector

class TestMainApp : MainApp(){

    override fun applicationInjector(): AndroidInjector<MainApp> =
            DaggerTestAppComponent.builder().create(this)
}
