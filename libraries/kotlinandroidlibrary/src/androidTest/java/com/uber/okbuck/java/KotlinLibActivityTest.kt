package com.uber.okbuck.java

import android.content.Intent
import org.junit.Rule
import org.junit.Test
import org.hamcrest.CoreMatchers.allOf
import android.support.test.rule.ActivityTestRule
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import com.uber.okbuck.kotlin.android.R

class KotlinLibActivityTest {

    @get:Rule
    val activityRule = ActivityTestRule(KotlinLibActivity::class.java, true, false)


    @Test
    fun testTextView() {
        activityRule.launchActivity(Intent())

        onView(allOf(withId(R.id.titleTV), withText("I'm in an android library test!"))).check(matches(isDisplayed()))
    }

}
