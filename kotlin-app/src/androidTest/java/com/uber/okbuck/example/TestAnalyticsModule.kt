package com.uber.okbuck.example

import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class TestAnalyticsModule {

    @Provides
    @Singleton
    fun provideAnalytics() : Analytics = TestAnalyticsImpl()
}
