package com.uber.okbuck.example

import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(
        modules = [
            AndroidSupportInjectionModule::class,
            TestAnalyticsModule::class,
            BindingModule::class
        ]
)
interface TestAppComponent: AndroidInjector<MainApp> {

    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<MainApp>()
}
