package com.uber.okbuck.example

import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Rule
import org.junit.Test

class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun daggerOnAndroidTestWillProvideFakeImpl() {
        activityRule.getScenario().onActivity { activity: MainActivity ->
        	assert(activity.analytics is TestAnalyticsImpl)
		}
    }
}
