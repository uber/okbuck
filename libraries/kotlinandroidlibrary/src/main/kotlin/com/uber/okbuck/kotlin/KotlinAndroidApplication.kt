package com.uber.oknuck.kotlin

import com.uber.oknuck.kotlin.di.DaggerDummyComponent
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication

class KotlinAndroidApplication : DaggerApplication() {

    override fun applicationInjector(): AndroidInjector<KotlinAndroidApplication> =
            DaggerDummyComponent.builder().create(this)
}
