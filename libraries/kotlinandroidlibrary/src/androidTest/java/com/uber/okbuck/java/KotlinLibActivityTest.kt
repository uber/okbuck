package com.uber.okbuck.java

import android.content.Intent
import org.junit.Rule
import org.junit.Test
import org.hamcrest.CoreMatchers.allOf
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.uber.okbuck.kotlin.android.R

class KotlinLibActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(KotlinLibActivity::class.java)

    @Test
    fun testTextView() {
    	activityRule.getScenario().onActivity { _ ->
        	onView(allOf(withId(R.id.titleTV), withText("I'm in an android library test!"))).check(matches(isDisplayed()))
		}
    }

}
