package com.uber.okbuck.example

import dagger.android.AndroidInjector
import dagger.android.DaggerApplication

open class MainApp : DaggerApplication(){

    override fun applicationInjector(): AndroidInjector<MainApp> =
        DaggerAppComponent.builder().create(this)
}
