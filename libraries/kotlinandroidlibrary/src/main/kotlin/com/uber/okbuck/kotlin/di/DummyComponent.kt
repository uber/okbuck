package com.uber.oknuck.kotlin.di

import com.uber.oknuck.kotlin.KotlinAndroidApplication
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(modules = [AndroidSupportInjectionModule::class])
interface DummyComponent : AndroidInjector<KotlinAndroidApplication> {
    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<KotlinAndroidApplication>()
}
