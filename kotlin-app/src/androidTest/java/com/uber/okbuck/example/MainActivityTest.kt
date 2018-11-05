package com.uber.okbuck.example

import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test

class MainActivityTest {

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java, true, true)

    @Test
    fun daggerOnAndroidTestWillProvideFakeImpl() {
        val activity: MainActivity = activityRule.activity
        assert(activity.analytics is TestAnalyticsImpl)
    }
}
